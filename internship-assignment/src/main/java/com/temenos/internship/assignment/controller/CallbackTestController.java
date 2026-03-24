package com.temenos.internship.assignment.controller;

import com.temenos.internship.assignment.model.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/callback")
public class CallbackTestController {

    private static final Logger logger =
            LoggerFactory.getLogger(CallbackTestController.class);


    // Simulated expected token (for testing only)
    private static final String EXPECTED_CSRF_TOKEN = "test-csrf-token";


    @PostMapping
    public ResponseEntity<Void> receiveCallback(@RequestBody Timer timer) {
        logger.info("✅ CALLBACK RECEIVED");
        if (!EXPECTED_CSRF_TOKEN.equals(timer.getCsrfToken())) {
            logger.warn(
                    "❌ Invalid CSRF token for timer {}",
                    timer.getTimerId()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } else {
            logger.info("Tokens match");
            logger.info("Timer ID   : {}", timer.getTimerId());
        }


        return ResponseEntity.ok().build();
    }

}