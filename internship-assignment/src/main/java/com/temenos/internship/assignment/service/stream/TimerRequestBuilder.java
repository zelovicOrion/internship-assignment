package com.temenos.internship.assignment.service.stream;

import com.temenos.internship.assignment.model.TimerRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TimerRequestBuilder {

    public TimerStreamRequest build(TimerRequest request) {
        return new TimerStreamRequest(
                UUID.randomUUID().toString(),
                request.getDelay(),
                System.currentTimeMillis()
        );
    }
}
