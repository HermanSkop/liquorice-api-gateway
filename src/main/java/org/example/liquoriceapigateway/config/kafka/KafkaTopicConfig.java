package org.example.liquoriceapigateway.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.liquorice-product-requests}")
    private String productRequestsTopic;

    @Bean
    public NewTopic productRequestsTopic() {
        return TopicBuilder.name(productRequestsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
