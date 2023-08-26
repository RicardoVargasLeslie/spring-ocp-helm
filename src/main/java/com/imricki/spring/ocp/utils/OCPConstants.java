package com.imricki.spring.ocp.utils;

public final class OCPConstants {

    public static final String RESOURCE_QUOTA_NAME = "resource-quota";
    public static final String LIMIT_RANGE_NAME = "limit-range";
    public static final String CPU_LIMIT = "500m"; // Change this value as needed
    public static final String MEMORY_LIMIT = "512Mi"; // Change this value as needed
    public static final String APPLICATION_LABEL_KEY = "app"; // Define the label key
    public static final String APPLICATION_LABEL_VALUE = "my-application"; // Define the label value
    public static final String SERVICE_ACCOUNT_NAME = "service-account-name";
    public static final String ROLE_NAME = "role-name";
    public static final String SECRET_NAME = "secret-name";
    public static final String SECRET_DATA_KEY = "secret-data-key";
    public static final String SECRET_DATA_VALUE = "secret-data-value";

    public static final String NETWORK_POLICY_YAML = "network.policy-yaml";

    private OCPConstants() {
        // Private constructor to prevent instantiation
    }
}
