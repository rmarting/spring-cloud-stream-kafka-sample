package com.jromanmartin.kafka.streams.service;

import com.jromanmartin.kafka.streams.model.Greetings;
import com.jromanmartin.kafka.streams.stream.GreetingsStreams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class GreetingsListener {

    @StreamListener(GreetingsStreams.INPUT)
    public void handleGreetings(@Payload Greetings greetings, @Headers Map<String, Object> headers) {
        log.info("Received greetings: {}. Partition: {}. Offset: {}", greetings,
                headers.get(KafkaHeaders.RECEIVED_PARTITION_ID), headers.get(KafkaHeaders.OFFSET));
    }

}
