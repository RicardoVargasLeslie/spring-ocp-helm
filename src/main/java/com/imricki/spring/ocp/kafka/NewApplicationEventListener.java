package com.imricki.spring.ocp.kafka;

import com.imricki.spring.ocp.event.NewApplicationEvent;
import com.imricki.spring.ocp.service.OpenshiftProvisioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NewApplicationEventListener {

    @Autowired
    OpenshiftProvisioner openshiftProvisioner;

    @KafkaListener(topics = "tp-application-topic", groupId = "consumer-1")
    public void listenToNewApplicationEvent(NewApplicationEvent newApplicationEvent) {
        // Process the Kafka message (NewApplication event)
        // You can trigger the namespace provisioning tasks here
        System.out.println("Received message: " + newApplicationEvent.getApplicationName());
        System.out.println("Recieved a Message: " + newApplicationEvent.getEnvironment());
        openshiftProvisioner.provisionResources(newApplicationEvent);
    }
}
