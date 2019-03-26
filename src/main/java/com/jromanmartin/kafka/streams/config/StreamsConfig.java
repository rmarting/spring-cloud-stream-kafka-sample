package com.jromanmartin.kafka.streams.config;

import com.jromanmartin.kafka.streams.stream.GreetingsStreams;
import org.springframework.cloud.stream.annotation.EnableBinding;

@EnableBinding(GreetingsStreams.class)
public class StreamsConfig {
}
