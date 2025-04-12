package org.example.liquoriceapigateway.config.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class CorrelationIdProducerInterceptor implements ProducerInterceptor<String, Object> {

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        Headers headers = record.headers();
        
        // Check if correlation ID already exists
        if (!hasCorrelationId(headers)) {
            // Generate a new correlation ID if none exists
            String correlationId = record.key() != null ? record.key() : UUID.randomUUID().toString();
            headers.add(KafkaHeaders.CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8));
        }
        
        return record;
    }

    private boolean hasCorrelationId(Headers headers) {
        return headers.headers(KafkaHeaders.CORRELATION_ID).iterator().hasNext();
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // No action needed
    }

    @Override
    public void close() {
        // No resources to clean up
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // No configuration needed
    }
}
