package org.example.liquoriceapigateway.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    
    @Value("${kafka.topics.orders:liquorice-orders}")
    private String ordersTopic;
    
    @Value("${kafka.topics.products:liquorice-products}")
    private String productsTopic;

    @Value("${kafka.topics.auth-requests:liquorice-auth-requests}")
    private String authRequestTopic;

    @Value("${kafka.topics.user-auth-requests:liquorice-user-auth-requests}")
    private String userAuthRequestTopic;

    @Value("${kafka.topics.response:liquorice-response}")
    private String responseTopic;

    @Bean
    public NewTopic requestTopic() {
        return TopicBuilder.name(authRequestTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic userAuthRequestTopic() {
        return TopicBuilder.name(userAuthRequestTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic responseTopic() {
        return TopicBuilder.name(responseTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(ordersTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic productsTopic() {
        return TopicBuilder.name(productsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
