package org.example.liquoriceapigateway.config.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka producer interceptor that adds a correlation ID to each message
 * if one doesn't already exist
 */
public class CorrelationIdProducerInterceptor implements ProducerInterceptor<String, Object> {

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        Headers headers = record.headers();

        if (!headers.headers("correlationId").iterator().hasNext()) {
            String correlationId = UUID.randomUUID().toString();
            headers.add(new RecordHeader("correlationId", correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // Not needed for this implementation
    }

    @Override
    public void close() {
        // Not needed for this implementation
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // Not needed for this implementation
    }
}
