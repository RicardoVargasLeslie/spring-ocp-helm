package com.imricki.spring.ocp.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NewApplicationEvent {

    private String applicationName;
    private String environment; // INT, CER, PRE, PRO, FOR
    // Add more attributes as needed
}
