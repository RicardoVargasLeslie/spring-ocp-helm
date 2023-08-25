package com.imricki.spring.ocp.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NewApplicationEventListener {

    @KafkaListener(topics = "new-application-topic", groupId = "consumer-1")
    public void listenToNewApplicationEvent(String message) {
        // Process the Kafka message (NewApplication event)
        // You can trigger the namespace provisioning tasks here
    }
}
