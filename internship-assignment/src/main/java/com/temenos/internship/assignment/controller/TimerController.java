package com.temenos.internship.assignment.controller;

import com.temenos.internship.assignment.service.TimerService;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;

import com.temenos.internship.assignment.api.TimerApi;
import com.temenos.internship.assignment.model.Timer;
import com.temenos.internship.assignment.model.TimerRequest;

@RestController
public class TimerController implements TimerApi {

    private static final Logger logger = LoggerFactory.getLogger(TimerController.class);
    private final TimerService timerService;

    public TimerController(TimerService timerService) {
        this.timerService = timerService;
    }

    @GetMapping("/test")
    Mono<ResponseEntity<String>> test() {
        logger.debug("Received request on /test endpoint");
        return Mono.just(ResponseEntity.ok("test ok"));
    }

    @Override
    public Mono<ResponseEntity<Timer>> createTimer(
            Mono<TimerRequest> timerRequest,
            ServerWebExchange exchange) {
        logger.debug("POST /api/timers - createTimer called");
        return timerRequest
                .flatMap(timerService::createTimer)
                .map(timer -> ResponseEntity.status(HttpStatus.CREATED).body(timer));
    }

    @Override
    public Mono<ResponseEntity<Flux<Timer>>> getAllTimers(
            ServerWebExchange exchange) {
        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity<Timer>> getTimerById(
            String timerId,
            ServerWebExchange exchange) {
        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteTimer(
            String timerId,
            ServerWebExchange exchange) {
        return Mono.empty();
    }
}
