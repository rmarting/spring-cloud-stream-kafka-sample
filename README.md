# Spring Cloud Stream with Kafka

## Overview

This sample project demonstrates how to use [Spring Cloud Stream](https://cloud.spring.io/spring-cloud-stream/) connected
with [Apache Kafka](https://kafka.apache.org/) managed by [Strimzi](https://strimzi.io/) in a [Kubernetes](https://kubernetes.io/) 
and [OpenShift](https://www.openshift.com/).

By end of this tutorial you'll have a simple Spring Boot based Greetings microservice running that

1. Deploy Kafka Cluster on Kubernetes or OpenShift Cluster
2. Takes a message from a REST API
3. Writes it to a Kafka topic
4. Reads it from the topic
5. Outputs it to the console

### What is Spring Cloud Streaming?

[Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) is a framework built upon Spring Boot for building message-driven microservices.

### What is Apache Kafka?

[Apache Kafka](https://kafka.apache.org/) is a popular high performance and horizontally scalable messaging platform originally developed by LinkedIn.

### What is Strimzi?

[Strimzi](https://strimzi.io/) provides a way to run an Apache Kafka cluster on OpenShift and Kubernetes in various deployment configurations.

### What is Kubernetes?

[Kubernetes](https://kubernetes.io/) is an open-source system for automating deployment, scaling, and management of 
containerized applications.

### What is OpenShift?

[OpenShift](https://www.openshift.com/) is combines application lifecycle management - including image builds, 
continuous integration, deployments, and updates - with Kubernetes.

### Kubernetes and OpenShift Platform

This tutorial requires a Kubernetes or OpenShift platform available. If you do not have one, you could use 
one of the following resources to deploy locally a Kubernetes or OpenShift Cluster:

* [minishift - run OpenShift locally](https://github.com/minishift/minishift)
* [Red Hat Container Development Kit](https://developers.redhat.com/products/cdk/overview/)  
* [minikube - Running Kubernetes Locally](https://kubernetes.io/docs/setup/minikube/)

## Let's get started!

### Deploying Kafka

Strimzi includes a set of Kubernetes Operators to deploy a full Kafka Cluster on a Kubernetes or OpenShift platform.

You can follow the instructions from [Community Documentation](https://strimzi.io/docs/latest/#downloads-str) or you
could use my [Ansible Playbook](https://github.com/rmarting/strimzi-ansible-playbook) to do it. In both cases it is 
very easy to do it.

```src/main/strimzi``` folder includes a set of custom resource definitions to deploy a Kafka Cluster
and a Kafka Topic using the Strimzi Operators.

To deploy the Kafka Cluster:

* Kubernetes:

```bash
$ kubectl apply -f src/main/strimz/kafka.yml -n namespace
kafka.kafka.strimzi.io/my-kafka created
```

* OpenShift:

```bash
$ oc apply -f src/main/strimz/kafka.yml -n namespace
kafka.kafka.strimzi.io/my-kafka created
```

To deploy the Kafka Topic:

* Kubernetes:

```bash
$ kubectl apply -f src/main/strimz/kafkatopic.yml -n namespace
kafkatopic.kafka.strimzi.io/greetings-sample created
```

* OpenShift:

```bash
$ oc apply -f src/main/strimz/kafkatopic.yml -n namespace
kafkatopic.kafka.strimzi.io/greetings-sample created
```

After some minutes Kafka Cluster will be deployed:

```bash
$ kubectl get pod
NAME                                           READY   STATUS    RESTARTS   AGE
my-kafka-entity-operator-8474bb6769-xqzt9      3/3     Running   0          1m
my-kafka-kafka-0                               2/2     Running   0          2m
my-kafka-kafka-1                               2/2     Running   0          2m
my-kafka-kafka-2                               2/2     Running   0          2m
my-kafka-kafka-exporter-5b4dff4858-8z9gw       1/1     Running   0          30s
my-kafka-zookeeper-0                           2/2     Running   0          3m
my-kafka-zookeeper-1                           2/2     Running   0          3m
my-kafka-zookeeper-2                           2/2     Running   0          3m
strimzi-cluster-operator-c8d786dcb-8rt9v       1/1     Running   2          5d
```

### Project Structure

Go to [Spring Initializer](https://start.spring.io) to create a Maven project:

1. Add necessary dependencies: ```Web```, ```Spring Cloud Stream```, ```Kafka```, ```Devtools```, ```Actuator```, ```Lombok```
2. Click the Generate Project button to download the project as a zip file
3. Extract zip file and import the maven project to your favourite IDE

Notice the maven dependencies in the ```pom.xml``` file:

* Spring Boot and Spring Cloud Dependencies

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-stream</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-stream-kafka</artifactId>
</dependency>

<!-- hot reload - press Ctrl+F9 in IntelliJ after a code change while application is running -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <optional>true</optional>
</dependency>
```

* Other Dependencies

```xml
<!-- Also install the Lombok plugin in your IDE -->
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <optional>true</optional>
</dependency>
```

These dependencies are managed by ```<dependencyManagement>``` section:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Define the Kafka streams

In order for our application to be able to communicate with Kafka, we'll need to define an outbound stream to write 
messages to a Kafka topic, and an inbound stream to read messages from a Kafka topic.

Spring Cloud provides a convenient way to do this by simply creating an interface that defines a separate method 
for each stream.

```java
package com.jromanmartin.kafka.streams.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface GreetingsStreams {

    String INPUT = "greetings-in";
    String OUTPUT = "greetings-out";

    @Input(INPUT)
    SubscribableChannel inboundGreetings();

    @Output(OUTPUT)
    MessageChannel outboundGreetings();
    
}
```

The ```inboundGreetings()``` method defines the inbound stream to read from Kafka and ```outboundGreetings()``` method defines 
the outbound stream to write to Kafka.

During runtime Spring will create a java proxy based implementation of the ```GreetingsStreams``` interface that 
can be injected as a Spring Bean anywhere in the code to access our two streams. 

### Configure Spring Cloud Stream

Our next step is to configure Spring Cloud Stream to bind to our streams in the ```GreetingsStreams``` interface. 

This can be done by creating a ```@Configuration``` class ```com.jromanmartin.kafka.streams.config.StreamsConfig``` with 
below code:

```java
package com.jromanmartin.kafka.streams.config;

import com.jromanmartin.kafka.streams.stream.GreetingsStreams;
import org.springframework.cloud.stream.annotation.EnableBinding;

@EnableBinding(GreetingsStreams.class)
public class StreamsConfig {
}
```

Binding the streams is done using the ```@EnableBinding``` annotation where the ```GreatingsService``` interface is passed to.

### Configuration properties for Kafka

By default, the configuration properties are stored in the ```src/main/resources/application.yaml``` file.

```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: my-kafka-kafka-bootstrap:9092
          auto-create-topics: false
          configuration:
            auto.offset.reset: latest
      bindings:
        greetings-in:
          destination: greetings
          group: greetings-in-group
          contentType: application/json
        greetings-out:
          destination: greetings
          contentType: application/json
```          

The above configuration properties configure the address of the Kafka server to connect to, and the Kafka topic we use 
for both the inbound and outbound streams in our code. They both must use the same Kafka topic!

Kafka brokers are defined by a Kubernetes or OpenShift service created by Strimzi when the Kafka cluster is deployed. This
service, called *cluster-name*-kafka-bootstrap exposes 9092 port for plain traffic and 9093 for encrypted traffic.  

The ```contentType``` properties tell Spring Cloud Stream to send/receive our message objects as ```String```s in the streams.

### Create the message object

Create a simple ```com.jromanmartin.kafka.streams.model.Greetings``` class with below code that will represent 
the message object we read from and write to the ```greetings``` Kafka topic:

```java
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
}
```

Notice how the class doesn't have any getters and setters thanks to the Lombok annotations. The ```@ToString``` will 
generate a ```toString()``` method using the class' fields and the ```@Builder``` annotation will allow us 
creating ```Greetings``` objects using fluent builder (see below).

### Create service layer to write to Kafka

Let's create the ```com.jromanmartin.kafka.streams.service.GreetingsService``` class with below code that will 
write a ```Greetings``` object to the ```greetings``` Kafka topic:

```java
package com.jromanmartin.kafka.streams.service;

import com.jromanmartin.kafka.streams.model.Greetings;
import com.jromanmartin.kafka.streams.stream.GreetingsStreams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Service
@Slf4j
public class GreetingsService {

    @Autowired
    private GreetingsStreams greetingsStreams;

    public void sendGreeting(final Greetings greetings) {
        log.info("Sending greetings {}", greetings);

        MessageChannel messageChannel = greetingsStreams.outboundGreetings();
        boolean sent = messageChannel.send(MessageBuilder
                .withPayload(greetings)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build());

        log.info("Sent {} greetings {}", sent, greetings);
    }

}
```

The ```@Service``` annotation will configure this class as a Spring Bean and inject the ```GreetingsService``` dependency via 
```@Autowired``` annotation.
The ```@Slf4j``` annotation will generate an SLF4J logger field that we can use for logging.

In the ```sendGreeting()``` method we use the injected ```GreetingsStream``` object to send a message represented by the ```Greetings``` object.

### Create REST API

Now we'll be creating a REST API endpoint that will trigger sending a message to Kafka using the ```GreetingsService``` Spring Bean:

```java
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
```

The ```@RestController``` annotation tells Spring that this is a Controller bean. The ```greetings()``` method defines 
an ```HTTP GET /greetings``` endpoint that takes a ```message``` request param and passes it to 
the ```sendGreeting()``` method in ```GreetingsService```.

### Listening on the greetings Kafka topic

Let's create a ```com.jromanmartin.kafka.streams.service.GreetingsListener``` class that will listen to messages 
on the ```greetings``` Kafka topic and log them on the console:

```java
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
```

The ```@Component``` annotation similarly to ```@Service``` and ```@RestController``` defines a Spring Bean.

```GreetingsListener``` has a single method, ```handleGreetings()``` that will be invoked by Spring Cloud Stream with 
every new ```Greetings``` message object on the ```greetings``` Kafka topic. This is thanks to the ```@StreamListener``` annotation 
configured for the ```handleGreetings()``` method.

```@Headers``` annotation inject Kafka record headers from the Kafka Topic. This map includes additional information from
the record as: partition id, offset, ...

### Running the application

The last piece of the puzzle is the ```com.jromanmartin.kafka.streams.StreamKafkaApplication``` class that was
auto-generated by the Spring Initializer:

```java
package com.jromanmartin.kafka.streams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StreamKafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamKafkaApplication.class, args);
    }
    
}
```

No need to make any changes here. You can either run this class as a Java application from your IDE, or run the application 
from the command line using the Spring Boot maven plugin:

```bash
$ mvn spring-boot:run
```

Or you can deploy into Kubernetes or OpenShift platform using [Eclipse JKube](https://github.com/eclipse/jkube) Maven Plug-ins: 

For Kubernetes:

```bash
$ mvn k8s:build k8s:resource k8s:apply -Pkubernetes
```

For OpenShift:

```bash
$ mvn oc:build oc:resource oc:apply -Popenshift
```

Once the application is running, go to ```http://<KUBERNETES_OPENSHIFT_HOST>/greetings?message=hello``` in the browser 
and check your console.

To get the route the following command in Kubernetes give you the host:

```bash
$ kubectl get route spring-cloud-stream-kafka-sample -o jsonpath='{.spec.host}'
```

In OpenShift:

```bash
$ oc get route spring-cloud-stream-kafka-sample -o jsonpath='{.spec.host}'
```

This command will send a message to the Kafka Topic:

```bash
curl http://$(oc get route spring-cloud-stream-kafka-sample -o jsonpath='{.spec.host}')//greetings?message=hello; echo
{"timestamp":1581086660762,"message":"hello"}
```

```text
2020-02-07 14:44:20.762  INFO 1 --- [nio-8080-exec-2] c.j.k.streams.service.GreetingsService   : Sending greetings Greetings(timestamp=1581086660762, message=hello)
2020-02-07 14:44:20.763  INFO 1 --- [nio-8080-exec-2] c.j.k.streams.service.GreetingsService   : Sent true greetings Greetings(timestamp=1581086660762, message=hello)
2020-02-07 14:44:20.770  INFO 1 --- [container-0-C-1] c.j.k.streams.service.GreetingsListener  : Received greetings: Greetings(timestamp=1581086660762, message=hello). Partition: 1. Offset: 1
```

### Summary

I hope you enjoyed this tutorial. Feel free to ask any questions and leave your feedback.
