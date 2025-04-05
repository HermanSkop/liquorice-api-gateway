package org.example.liquoriceapigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class LiquoriceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LiquoriceApplication.class, args);
    }
}
