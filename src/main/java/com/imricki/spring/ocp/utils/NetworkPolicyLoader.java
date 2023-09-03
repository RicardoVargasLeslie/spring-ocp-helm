package com.imricki.spring.ocp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Slf4j
@Component
public class NetworkPolicyLoader {

    public String loadNetworkPolicyYaml() {

        // Use the classloader to load the YAML file as an InputStream
        InputStream inputStream = getClass().getResourceAsStream("/network-policy.yml");

        if (inputStream != null) {
            // Read the contents of the YAML file
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                StringBuilder yamlContent = new StringBuilder();
                while (scanner.hasNextLine()) {
                    yamlContent.append(scanner.nextLine()).append("\n");
                }
                return yamlContent.toString();
            } catch (Exception e) {
                // Handle any exceptions that may occur while reading the file
                log.error("Error Reading YAML file: " + e.getMessage());
            } finally {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // Handle any exceptions that may occur while closing the stream
                    log.error("Error while closing the stream: " + e.getMessage());
                }
            }
        } else {
            // Handle the case where the YAML file is not found
            log.error("NetworkPolicy YAML file not found.");
        }

        return null;
    }
}
