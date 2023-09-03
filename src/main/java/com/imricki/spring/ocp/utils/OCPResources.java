package com.imricki.spring.ocp.utils;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class OCPResources {

    @Value("${openshift.api.url}") // Define this property in your application.properties or application.yml
    private String openshiftApiUrl;

    @Value("${resource-quota-name}")
    private String resourceQuotaName;

    @Value("${limit-range-name}")
    private String limitRangeName;

    @Value("${cpu-limit}")
    private String cpuLimit;

    @Value("${memory-limit}")
    private String memoryLimit;

    @Value("${application-label-key}")
    private String applicationLabelKey;

    @Value("${application-label-value}")
    private String applicationLabelValue;

    @Value("${service-account-name}")
    private String serviceAccountName;

    @Value("${role-name}")
    private String roleName;

    @Value("${secret-name}")
    private String secretName;

    @Value("${secret-data-key}")
    private String secretDataKey;

    @Value("${secret-data-value}")
    private String secretDataValue;

    @Value("${network-policy}")
    private String networkPolicy;

    @Value("${network-key}")
    private String networkKey;

    @Value("${network-value}")
    private String networkValue;

    private OCPResources() {
        // Private constructor to prevent instantiation
    }
}
