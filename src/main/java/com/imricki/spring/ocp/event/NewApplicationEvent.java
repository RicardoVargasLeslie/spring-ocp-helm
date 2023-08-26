package com.imricki.spring.ocp.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewApplicationEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    private String applicationName;
    private String environment; // INT, CER, PRE, PRO, FOR
    // Add more attributes as needed
}
