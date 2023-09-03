package com.imricki.spring.ocp.controller;

import com.imricki.spring.ocp.event.NewApplicationEvent;
import com.imricki.spring.ocp.service.OpenshiftProvisioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocp")
public class OpenshiftController {

    @Autowired
    private OpenshiftProvisioner openshiftProvisioner;

    @GetMapping("/provisionresources")
    public ResponseEntity<?> provisionResources() {

        NewApplicationEvent event = new NewApplicationEvent();
        event.setEnvironment("pre");
        event.setApplicationName("realme");

        openshiftProvisioner.provisionResources(event);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
