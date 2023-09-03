package com.imricki.spring.ocp.utils;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

@Slf4j
@Component
public class ProvisioningUtils {

    private final KubernetesClient kubernetesClient;

    @Autowired
    private OCPResources ocpResources;

    @Autowired
    public ProvisioningUtils(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
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
                .withName("ot-limit-range") // Replace with a valid name
                .endMetadata()
                .withNewSpec()
                .withLimits(limitRangeItem)
                .endSpec()
                .build();

        // Check if the LimitRange already exists by trying to get it
        LimitRange existingLimitRange = kubernetesClient.limitRanges().inNamespace(namespaceName)
                .withName("ot-limit-range") // Replace with a valid name
                .get();

        if (existingLimitRange != null) {
            // LimitRange already exists, replace it
            kubernetesClient.limitRanges().inNamespace(namespaceName)
                    .withName("ot-limit-range") // Replace with a valid name
                    .replace(limitRange);
            System.out.println("LimitRange replaced successfully.");
        } else {
            // LimitRange does not exist, create it
            kubernetesClient.limitRanges().inNamespace(namespaceName)
                    .create(limitRange);
            System.out.println("LimitRange created successfully.");
        }

        // Create ResourceQuota
        ResourceQuota resourceQuota = new ResourceQuotaBuilder()
                .withNewMetadata()
                .withName("ot-resource-quota") // Replace with a valid name
                .withNamespace(namespaceName)
                .endMetadata()
                .withNewSpec()
                .addToHard("limits.cpu", cpuQuantity)
                .addToHard("limits.memory", memoryQuantity)
                .endSpec()
                .build();

        kubernetesClient.resourceQuotas().inNamespace(namespaceName)
                .createOrReplace(resourceQuota);

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

        kubernetesClient.rbac().roleBindings().inNamespace(namespaceName).createOrReplace(roleBinding);
        log.info("RoleBinding created successfully...");
    }

    /*
    This method is responsible for creating a Kubernetes Secret within the specified namespace.
     Secrets are used to securely store sensitive information,
     */
    public void createSecrets(String namespaceName) {

        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName(ocpResources.getSecretName())
                .withNamespace(namespaceName)
                .endMetadata()
                .withType("Opaque") // Change this to the appropriate type if needed
                .addToData(ocpResources.getSecretDataKey(), ocpResources.getSecretDataValue())
                .build();

        kubernetesClient.secrets().inNamespace(namespaceName).createOrReplace(secret);

        log.info("Secret created successfully...");
    }

    /*
    This method is responsible for creating a Kubernetes NetworkPolicy within the specified namespace.
     Network policies are used to control and define communication between pods
     */
    public void createNetworkPolicies(String namespaceName) {
        try {
            Process process = new ProcessBuilder("kubectl", "apply", "-f", ocpResources.getNetworkPolicyYaml(),
                    "--namespace=" + namespaceName)
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("NetworkPolicy applied successfully...");
            } else {
                log.info("Failed to apply NetworkPolicy...");
            }
        } catch (IOException | InterruptedException e) {
            log.error("There was a error creating Network Policy", e);
        }
    }

    public void executeHelmChart(String namespaceName) {

        final String HELM_COMMAND = "helm"; // Change this to the path of the Helm executable
        final String HELM_CHART_NAME = "my-helm-chart"; // Change this to your Helm chart name

        try {
            Process process = new ProcessBuilder(
                    HELM_COMMAND, "install", HELM_CHART_NAME,
                    "--namespace", namespaceName
            ).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Helm chart installation successful...");
            } else {
                log.info("Helm chart installation failed...");
            }
        } catch (Exception e) {
            log.error("There was a executeHelmChart creation", e);
        }
    }
    public void reportResults(String namespaceName) {
        log.info("Report results (logging, Kafka topic, API endpoint, etc....");
    }
}
