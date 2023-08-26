package com.imricki.spring.ocp;

import com.imricki.spring.ocp.event.NewApplicationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@SpringBootApplication
public class OcpApplication  {

	public static void main(String[] args) {
		SpringApplication.run(OcpApplication.class, args);
	}
}
