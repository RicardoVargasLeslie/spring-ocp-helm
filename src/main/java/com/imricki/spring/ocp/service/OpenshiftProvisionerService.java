package com.imricki.spring.ocp.service;

import com.imricki.spring.ocp.event.NewApplicationEvent;
import com.imricki.spring.ocp.utils.ProvisioningUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenshiftProvisionerService implements OpenshiftProvisioner {

    private final ProvisioningUtils provisioningUtils;
    @Autowired
    public OpenshiftProvisionerService(ProvisioningUtils provisioningUtils) {
        this.provisioningUtils = provisioningUtils;
    }
    @Override
    public void provisionResources(final NewApplicationEvent newApplicationEvent) {

        // Extract application details from newApplication object
        String applicationName = newApplicationEvent.getApplicationName();
        String environment = newApplicationEvent.getEnvironment();
        // Define resource names and configurations based on application details
        String namespaceName = environment + "-" + applicationName;

        log.info("Create Namespace...");
        provisioningUtils.createNamespace(namespaceName);
        log.info("Create Limits/Quotas...");
        provisioningUtils.createLimitsAndQuotas(namespaceName);
        log.info("Create ServiceAccount...");
        provisioningUtils.createServiceAccount(namespaceName);
        log.info("Create RoleBindings...");
        provisioningUtils.createRoleBindings(namespaceName);
        log.info("Create Secrets...");
        provisioningUtils.createSecrets(namespaceName);
        log.info("Create NetworkPolicies...");
        provisioningUtils.createNetworkPolicies(namespaceName);
        log.info("Execute Helm Chart...");
        provisioningUtils.executeHelmChart(namespaceName);
        log.info("Report results (logging, Kafka topic, API endpoint, etc....");
        provisioningUtils.reportResults(namespaceName);
    }
}
