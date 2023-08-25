package com.imricki.spring.ocp.service;

import com.imricki.spring.ocp.event.NewApplicationEvent;

public interface OpenshiftProvisioner {
     void provisionResources(NewApplicationEvent newApplicationEvent);
}
