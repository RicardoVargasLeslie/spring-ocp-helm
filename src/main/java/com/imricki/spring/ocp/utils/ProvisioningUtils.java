package com.imricki.spring.ocp.utils;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ProvisioningUtils {

    @Value("${openshift.api.url}") // Define this property in your application.properties or application.yml
    private String openshiftApiUrl;

    private final KubernetesClient kubernetesClient;

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
        metadata.setLabels(Collections.singletonMap(OCPConstants.APPLICATION_LABEL_KEY, OCPConstants.APPLICATION_LABEL_VALUE));
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

        Quantity cpuQuantity = new Quantity(OCPConstants.CPU_LIMIT);
        Quantity memoryQuantity = new Quantity(OCPConstants.MEMORY_LIMIT);

        // Create ResourceQuota
        ResourceQuota resourceQuota = new ResourceQuotaBuilder()
                .withNewMetadata()
                .withName(OCPConstants.RESOURCE_QUOTA_NAME)
                .withNamespace(namespaceName)
                .endMetadata()
                .withNewSpec()
                .addToHard("limits.cpu", cpuQuantity)
                .addToHard("limits.memory", memoryQuantity)
                .endSpec()
                .build();

        kubernetesClient.resourceQuotas().inNamespace(namespaceName).createOrReplace(resourceQuota);

        // Create LimitRange
        LimitRangeItem cpuLimitItem = new LimitRangeItemBuilder()
                .withDefaultRequest(Collections.singletonMap("cpu", cpuQuantity))
                .withMax(Collections.singletonMap("cpu", cpuQuantity))
                .build();

        LimitRangeItem memoryLimitItem = new LimitRangeItemBuilder()
                .withDefaultRequest(Collections.singletonMap("memory", memoryQuantity))
                .withMax(Collections.singletonMap("memory", memoryQuantity))
                .build();

        Map<String, String> labels = new HashMap<>();
        labels.put(OCPConstants.APPLICATION_LABEL_KEY, OCPConstants.APPLICATION_LABEL_VALUE);

        LimitRange limitRange = new LimitRangeBuilder()
                .withNewMetadata()
                .withName(OCPConstants.LIMIT_RANGE_NAME)
                .withNamespace(namespaceName)
                .withLabels(labels) // Set the labels here
                .endMetadata()
                .withNewSpec()
                .withLimits(cpuLimitItem, memoryLimitItem)
                .endSpec()
                .build();

        kubernetesClient.limitRanges().inNamespace(namespaceName).createOrReplace(limitRange);

        log.info("ResourceQuota and LimitRange created successfully...");
    }

    /*
     This method creates a new Kubernetes service account within the given namespace.
     It uses the ServiceAccountBuilder to define and configure the service account.
    */
    public void createServiceAccount(String namespaceName) {
        ServiceAccount serviceAccount = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(OCPConstants.SERVICE_ACCOUNT_NAME)
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
                .withName("rolebinding-" + OCPConstants.SERVICE_ACCOUNT_NAME)
                .withNamespace(namespaceName)
                .endMetadata()
                .withRoleRef(new RoleRefBuilder()
                        .withKind("Role") // This could also be "ClusterRole"
                        .withName(OCPConstants.ROLE_NAME)
                        .build())
                .addNewSubject()
                .withKind("ServiceAccount")
                .withName(OCPConstants.SERVICE_ACCOUNT_NAME)
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
                .withName(OCPConstants.SECRET_NAME)
                .withNamespace(namespaceName)
                .endMetadata()
                .withType("Opaque") // Change this to the appropriate type if needed
                .addToData(OCPConstants.SECRET_DATA_KEY, OCPConstants.SECRET_DATA_VALUE)
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
            Process process = new ProcessBuilder("kubectl", "apply", "-f", OCPConstants.NETWORK_POLICY_YAML,
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
