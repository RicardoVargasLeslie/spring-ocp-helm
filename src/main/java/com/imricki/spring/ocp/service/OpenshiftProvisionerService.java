package com.imricki.spring.ocp.service;

import com.imricki.spring.ocp.event.NewApplicationEvent;
import com.imricki.spring.ocp.utils.ProvisioningUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpenshiftProvisionerService implements OpenshiftProvisioner {


    @Autowired
    private ProvisioningUtils provisioningUtils;


    @Override
    public void provisionResources(final NewApplicationEvent newApplicationEvent) {

        // Extract application details from newApplication object
        String applicationName = newApplicationEvent.getApplicationName();
        String environment = newApplicationEvent.getEnvironment();

        // Define resource names and configurations based on application details
        String namespaceName = environment + "-" + applicationName;
        // Create Namespace
        provisioningUtils.createNamespace(namespaceName);
        // Create Limits/Quotas
        provisioningUtils.createLimitsAndQuotas(namespaceName);
        // Create ServiceAccount
        provisioningUtils.createServiceAccount(namespaceName);
        // Create RoleBindings
        provisioningUtils.createRoleBindings(namespaceName);
        // Create Secrets
        provisioningUtils.createSecrets(namespaceName);
        // Create NetworkPolicies
        provisioningUtils.createNetworkPolicies(namespaceName);
        // Execute Helm Chart
        provisioningUtils.executeHelmChart(namespaceName);
        // Report results (logging, Kafka topic, API endpoint, etc.)
        provisioningUtils.reportResults(namespaceName);
    }
}
