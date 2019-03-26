package com.jromanmartin.kafka.streams.model;

// lombok autogenerates getters, setters, toString() and a builder (see https://projectlombok.org/):
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @Builder
public class Greetings {

    private long timestamp;
    private String message;
    private int partition;
    private long offset;

    public Greetings() {
    }

    public Greetings(long timestamp, String message, int partition, long offset) {
        this.timestamp = timestamp;
        this.message = message;
        this.partition = partition;
        this.offset = offset;
    }

}
