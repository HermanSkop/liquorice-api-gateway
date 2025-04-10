package org.example.liquoriceapigateway.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
@Getter
@Setter
public class KafkaConfig {
    private String bootstrapServers;

    @Value("${kafka.topics.product-replies}")
    private String productRepliesTopic;

    @Value("${kafka.consumer.trusted-packages}")
    private String trustedPackages;

    @Value("${kafka.consumer.use-type-info-headers}")
    private Boolean useTypeInfoHeaders;

    @Value("${kafka.consumer.remove-type-info-headers}")
    private Boolean removeTypeInfoHeaders;

    @Value("${kafka.consumer.poll-timeout}")
    private Integer pollTimeout;

    @Value("${kafka.reply.group-id}")
    private String replyGroupId;

    @Value("${kafka.reply.timeout-seconds}")
    private Integer replyTimeoutSeconds;
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.TYPE_MAPPINGS,
                "getCategoriesRequest:org.example.liquoriceapigateway.dtos.product.request.GetCategoriesRequest," +
                "getProductsRequest:org.example.liquoriceapigateway.dtos.product.request.GetProductsRequest," +
                "setAvailabilityRequest:org.example.liquoriceapigateway.dtos.product.request.SetAvailabilityRequest");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public ReplyingKafkaTemplate<String, Object, ?> replyingKafkaTemplate(
            ProducerFactory<String, Object> pf,
            ConcurrentMessageListenerContainer<String, Object> repliesContainer) {
        ReplyingKafkaTemplate<String, Object, ?> template =
            new ReplyingKafkaTemplate<>(pf, repliesContainer);
        template.setDefaultReplyTimeout(Duration.ofSeconds(replyTimeoutSeconds));
        return template;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Object> repliesContainer(
            ConcurrentKafkaListenerContainerFactory<String, Object> factory) {
        ConcurrentMessageListenerContainer<String, Object> container =
            factory.createContainer(productRepliesTopic);
        container.getContainerProperties().setGroupId(replyGroupId);
        container.getContainerProperties().setLogContainerConfig(true);
        return container;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(JsonDeserializer.TYPE_MAPPINGS,
                "org.example.liqouriceproductservice.dtos.response.GetCategoriesResponse:org.example.liquoriceapigateway.dtos.product.response.GetCategoriesResponse," +
                "getCategoriesResponse:org.example.liquoriceapigateway.dtos.product.response.GetCategoriesResponse," +
                "getProductsResponse:org.example.liquoriceapigateway.dtos.product.response.GetProductsResponse," +
                "setAvailabilityResponse:org.example.liquoriceapigateway.dtos.product.response.SetAvailabilityResponse," +
                "productPreviewDto:org.example.liquoriceapigateway.dtos.ProductPreviewDto");

        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(Object.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setPollTimeout(pollTimeout);
        return factory;
    }

    @Bean
    public ReceiverOptions<String, Object> reactiveReceiverOptions() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return ReceiverOptions.create(props);
    }
}
