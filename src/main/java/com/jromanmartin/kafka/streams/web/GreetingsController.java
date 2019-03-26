package com.jromanmartin.kafka.streams.web;

import com.jromanmartin.kafka.streams.model.Greetings;
import com.jromanmartin.kafka.streams.service.GreetingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingsController {

    @Autowired
    private GreetingsService greetingsService;

    @GetMapping("/greetings")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<Greetings> greetings(@RequestParam("message") String message) {
        Greetings greetings = Greetings.builder()
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();

        greetingsService.sendGreeting(greetings);

        return ResponseEntity.ok(greetings);
    }

}
