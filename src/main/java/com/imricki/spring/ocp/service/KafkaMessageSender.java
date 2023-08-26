package com.imricki.spring.ocp.service;

import com.imricki.spring.ocp.event.NewApplicationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageSender implements ApplicationRunner {

    private final KafkaTemplate<String, NewApplicationEvent> kafkaTemplate;

    @Autowired
    public KafkaMessageSender(KafkaTemplate<String, NewApplicationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    @Override
    public void run(ApplicationArguments args) {

        String topic = "tp-application-topic"; // Replace with your topic name

        NewApplicationEvent newApplicationEvent =new NewApplicationEvent();
        newApplicationEvent.setEnvironment("PROD");
        newApplicationEvent.setApplicationName("invento");

        kafkaTemplate.send(topic,"key",newApplicationEvent);
        System.out.println("Test message sent to Kafka topic: " + topic);

    }
}
