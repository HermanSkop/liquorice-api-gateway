package org.example.liquoriceapigateway.kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ReactiveKafkaConsumerService {

    private final ReactiveKafkaProducerService producerService;
    private final String bootstrapServers;
    private final String responseTopics;
    private final String groupId;
    
    private KafkaReceiver<String, Object> receiver;
    
    public ReactiveKafkaConsumerService(
            ReactiveKafkaProducerService producerService,
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.topics.response}") String responseTopics,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        this.producerService = producerService;
        this.bootstrapServers = bootstrapServers;
        this.responseTopics = responseTopics;
        this.groupId = groupId + "-reactive";
    }
    
    @PostConstruct
    public void init() {
        // Configure and start the reactive consumer
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        ReceiverOptions<String, Object> receiverOptions = ReceiverOptions.<String, Object>create(props)
                .subscription(Collections.singleton(responseTopics));
        
        receiver = KafkaReceiver.create(receiverOptions);
        
        // Start consuming messages and route them to the producer service
        receiver.receive()
                .subscribe(record -> {
                    String correlationId = null;
                    if (record.headers().lastHeader("correlationId") != null) {
                        correlationId = new String(record.headers().lastHeader("correlationId").value(), 
                                StandardCharsets.UTF_8);
                    } else if (record.key() != null) {
                        correlationId = record.key();
                    }
                    
                    if (correlationId != null) {
                        log.debug("Received reactive response for correlationId: {}", correlationId);
                        producerService.completeResponse(correlationId, record.value());
                    } else {
                        log.warn("Received reactive message without correlation ID: {}", record.value());
                    }
                    
                    // Acknowledge the message
                    record.receiverOffset().acknowledge();
                }, error -> {
                    log.error("Error in reactive Kafka consumer", error);
                });
    }
    
    @PreDestroy
    public void shutdown() {
        // Clean shutdown if needed
    }
}
