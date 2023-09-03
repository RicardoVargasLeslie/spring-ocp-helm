package com.imricki.spring.ocp.utils;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ProvisioningUtils {

    private final KubernetesClient kubernetesClient;
    private final OCPResources ocpResources;
    private final NetworkPolicyLoader networkPolicyLoader;

    @Autowired
    public ProvisioningUtils(final KubernetesClient kubernetesClient, final OCPResources ocpResources, final NetworkPolicyLoader networkPolicyLoader) {
        this.kubernetesClient = kubernetesClient;
        this.ocpResources = ocpResources;
        this.networkPolicyLoader = networkPolicyLoader;
    }

    /*
         Creates a new Kubernetes namespace with the provided name.
         It uses the NamespaceBuilder to construct a new Namespace object
         and then uses the Kubernetes client to create or replace the namespace in the cluster.
        */
    public void createNamespace(String namespaceName) {

        Namespace newNamespace = new Namespace();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(namespaceName);
        metadata.setLabels(Collections.singletonMap(ocpResources.getApplicationLabelKey(), ocpResources.getApplicationLabelValue()));
        newNamespace.setMetadata(metadata);
        kubernetesClient.namespaces().createOrReplace(newNamespace);
        log.info("Namespace created successfully...");
    }

    /*
     This method creates resource quotas and limit ranges for CPU and memory in the specified namespace.
     It utilizes the ResourceQuotaBuilder and LimitRangeBuilder
     to create and configure these resource management policies.
   */
    public void createLimitsAndQuotas(String namespaceName) {

        Quantity cpuQuantity = new Quantity(ocpResources.getCpuLimit());
        Quantity memoryQuantity = new Quantity(ocpResources.getMemoryLimit());

        // Create or load your LimitRange object
        LimitRangeItem limitRangeItem = new LimitRangeItem();
        limitRangeItem.setType("Container");
        limitRangeItem.setDefaultRequest(Collections.singletonMap("cpu", new Quantity(ocpResources.getCpuLimit())));
        limitRangeItem.setDefault(Collections.singletonMap("cpu", new Quantity(ocpResources.getCpuLimit())));

        LimitRange limitRange = new LimitRangeBuilder()
                .withNewMetadata()
                .withName(ocpResources.getLimitRangeName()) // Replace with a valid name
                .endMetadata()
                .withNewSpec()
                .withLimits(limitRangeItem)
                .endSpec()
                .build();

        // Check if the LimitRange already exists by trying to get it
        LimitRange existingLimitRange = kubernetesClient.limitRanges().inNamespace(namespaceName)
                .withName(ocpResources.getLimitRangeName()) // Replace with a valid name
                .get();

        if (existingLimitRange != null) {
            log.info("LimitRange already exists");
        } else {
            // LimitRange does not exist, create it
            kubernetesClient.limitRanges().inNamespace(namespaceName)
                    .create(limitRange);
            log.info("LimitRange created successfully.");
        }

        // Create ResourceQuota
        ResourceQuota resourceQuota = new ResourceQuotaBuilder()
                .withNewMetadata()
                .withName(ocpResources.getResourceQuotaName()) // Replace with a valid name
                .withNamespace(namespaceName)
                .endMetadata()
                .withNewSpec()
                .addToHard("limits.cpu", cpuQuantity)
                .addToHard("limits.memory", memoryQuantity)
                .endSpec()
                .build();

        ResourceQuota existingResourceQuota = kubernetesClient.resourceQuotas().inNamespace(namespaceName)
                .withName(ocpResources.getResourceQuotaName())
                .get();

        if (existingResourceQuota != null) {
            log.info("LimitRange already exists.");
        } else {
            // LimitRange does not exist, create it
            kubernetesClient.resourceQuotas().inNamespace(namespaceName)
                    .create(resourceQuota);
            log.info("LimitRange created successfully.");
        }

        log.info("ResourceQuota and LimitRange created successfully...");
    }
    /*
     This method creates a new Kubernetes service account within the given namespace.
     It uses the ServiceAccountBuilder to define and configure the service account.
    */
    public void createServiceAccount(String namespaceName) {

        ServiceAccount serviceAccount = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(ocpResources.getServiceAccountName())
                .withNamespace(namespaceName)
                .endMetadata()
                .build();

        kubernetesClient.serviceAccounts().inNamespace(namespaceName).createOrReplace(serviceAccount);
        log.info("ServiceAccount created successfully...");
    }

    /*
    This method creates a role binding for a specific service account and role (or cluster role).
    It uses the RoleBindingBuilder to construct the role binding, specifying the service account and role reference.
   */
    public void createRoleBindings(String namespaceName) {

        RoleBinding roleBinding = new RoleBindingBuilder()
                .withNewMetadata()
                .withName("rolebinding-" + ocpResources.getServiceAccountName())
                .withNamespace(namespaceName)
                .endMetadata()
                .withRoleRef(new RoleRefBuilder()
                        .withKind("Role") // This could also be "ClusterRole"
                        .withName(ocpResources.getRoleName())
                        .build())
                .addNewSubject()
                .withKind("ServiceAccount")
                .withName(ocpResources.getServiceAccountName())
                .endSubject()
                .build();

        RoleBinding existingBinding = kubernetesClient.rbac().roleBindings().inNamespace(namespaceName).createOrReplace(roleBinding);

        if (existingBinding != null) {
            // RoleBinding already exists
            log.info("RoleBinding already exists");
        } else {
            // Secret does not exist, create it
            kubernetesClient.rbac().roleBindings().inNamespace(namespaceName).create(roleBinding);
            log.info("RoleBinding created successfully.");
        }

        log.info("RoleBinding Finished OK...");
    }

    /*
    This method is responsible for creating a Kubernetes Secret within the specified namespace.
     Secrets are used to securely store sensitive information,
     */
    public void createSecrets(String namespaceName) {

        try {
            // Encode the secret data to base64
            String encodedSecretValue = Base64.getEncoder().encodeToString(ocpResources.getSecretDataValue().getBytes());

            Secret secret = new SecretBuilder()
                    .withNewMetadata()
                    .withName(ocpResources.getSecretName())
                    .withNamespace(namespaceName)
                    .endMetadata()
                    .withType("Opaque") // Change this to the appropriate type if needed
                    .addToData(ocpResources.getSecretDataKey(), encodedSecretValue)
                    .build();

            // Check if the Secret already exists by trying to get it
            Secret existingSecret = kubernetesClient.secrets().inNamespace(namespaceName).withName(ocpResources.getSecretName()).get();
            if (existingSecret != null) {
                log.info("Secret already exists");
            } else {
                // Secret does not exist, create it
                kubernetesClient.secrets().inNamespace(namespaceName).create(secret);
                log.info("Secret created successfully.");
            }

            log.info("Secret created or replaced successfully...");
        } catch (Exception e) {
            log.error("Error creating or replacing Secret: " + e.getMessage());
            log.error("StackTrace: " + Arrays.toString(e.getStackTrace()));

        }
    }

    /*
    This method is responsible for creating a Kubernetes NetworkPolicy within the specified namespace.
     Network policies are used to control and define communication between pods
     */
    public void createNetworkPolicies(String namespaceName) {

        Resource<NetworkPolicy> networkPolicyResource = kubernetesClient.network().networkPolicies().inNamespace(namespaceName)
                .withName(ocpResources.getNetworkPolicy());

        NetworkPolicy networkPolicy = new NetworkPolicyBuilder()
                .withNewMetadata()
                .withName("my-network-policy") // Replace with your desired name
                .withNamespace(namespaceName)
                .endMetadata()
                .withNewSpec()
                .addNewIngress()
                .addNewFrom()
                .withNewPodSelector()
                .addToMatchLabels("network-key", "network-value") // Replace with your label selector
                .endPodSelector()
                .endFrom()
                .addNewPort()
                .withNewPort(80) // Replace with your desired port
                .endPort()
                .endIngress()
                .endSpec()
                .build();

        if (networkPolicyResource != null) {
            // NetworkPolicy already exists
            log.info("NetworkPolicy already exists.");

            kubernetesClient.network().networkPolicies().inNamespace(namespaceName)
                    .withName("network-policy").createOrReplace(networkPolicy);
        } else {
            // NetworkPolicy does not exist, create it

            kubernetesClient.network().networkPolicies().inNamespace(namespaceName)
                    .create(networkPolicy);
            log.info("NetworkPolicy created successfully.");
        }

    }

    public static void executeHelmChart(String namespaceName) {

/*            String releaseName = "my-helm-release"; // Change this to your desired release name
            String chartName = "stable/nginx-ingress"; // Change this to your Helm chart name

            // Check if Helm release already exists
            HelmRelease existingRelease = kubernetesClient.customResources(HelmRelease.class)
                    .inNamespace(namespaceName)
                    .withName(releaseName)
                    .get();

            if (existingRelease != null) {
                // Helm release already exists, perform an upgrade
                HelmReleaseSpec spec = existingRelease.getSpec();
                spec.setChart(chartName);
                existingRelease.setSpec(spec);
                kubernetesClient.customResources(HelmRelease.class)
                        .inNamespace(namespaceName)
                        .withName(releaseName)
                        .edit()
                        .replace(existingRelease);
                System.out.println("Helm chart upgrade successful...");
            } else {
                // Helm release does not exist, create it
                HelmRelease newRelease = new HelmReleaseBuilder()
                        .withNewMetadata()
                        .withName(releaseName)
                        .withNamespace(namespaceName)
                        .endMetadata()
                        .withNewSpec()
                        .withChart(chartName)
                        .endSpec()
                        .build();
                kubernetesClient.customResources(HelmRelease.class)
                        .inNamespace(namespaceName)
                        .createOrReplace(newRelease);
                System.out.println("Helm chart installation successful...");
            }*/
    }

    public void reportResults(String namespaceName) {
        log.info("Report results (logging, Kafka topic, API endpoint, etc....");
    }
}
