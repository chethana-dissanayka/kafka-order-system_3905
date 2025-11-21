package com.kafka.assignment;

import org.apache.kafka.clients.producer.*;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

import java.util.Properties;

public class OrderProducer {

    private final KafkaProducer<String, Order> producer;
    private final String ordersTopic;

    public OrderProducer() {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                Config.get("bootstrap.servers"));
        props.put("schema.registry.url",
                Config.get("schema.registry.url"));

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaAvroSerializer.class);

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        this.producer = new KafkaProducer<>(props);
        this.ordersTopic = Config.get("topic.orders");
    }

    public void send(Order order) {
        sendToTopic(ordersTopic, order);
    }

    public void sendToTopic(String topic, Order order) {
        ProducerRecord<String, Order> record =
                new ProducerRecord<>(topic, (String) order.getOrderId(), order);

        producer.send(record, (metadata, exception) -> {
            if (exception == null)
                System.out.println("Produced to " + topic + ": " + order);
            else
                System.err.println("Error: " + exception.getMessage());
        });
    }
}
