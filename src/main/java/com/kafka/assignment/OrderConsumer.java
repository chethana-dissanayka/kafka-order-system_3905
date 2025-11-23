package com.kafka.assignment;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OrderConsumer {

    public static void main(String[] args) {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Config.get("bootstrap.servers"));
        props.put("schema.registry.url", Config.get("schema.registry.url"));
        props.put("specific.avro.reader", "true");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");

        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                KafkaAvroDeserializer.class);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, Order> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singleton(Config.get("topic.orders")));

        while (true) {

            ConsumerRecords<String, Order> records = consumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, Order> rec : records) {
                Order order = rec.value();

                try {
                    process(order);
                } catch (Exception e) {
                    System.err.println("Error processing order: " + e.getMessage());
                }
            }
        }
    }

    private static void process(Order order) {
        String orderId = order.getOrderId().toString();
        OrderValidator.ValidationResult result = OrderValidator.validateOrderId(orderId);

        switch (result) {
            case VALID:
                System.out.println("✓ Processed: " + order);
                // Real-time Price Aggregation - Calculate running average
                PriceAggregator.addPrice(order.getPrice());
                break;

            case RETRY:
                System.out.println("Retrying order " + order);
                sendToTopic(Config.get("topic.retry"), order);
                break;

            case DLQ:
                System.out.println("Sending to DLQ : " + order);
                sendToTopic(Config.get("topic.dlq"), order);
                break;
        }
    }

    private static void sendToTopic(String topic, Order order) {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Config.get("bootstrap.servers"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaAvroSerializer.class);
        props.put("schema.registry.url", Config.get("schema.registry.url"));

        KafkaProducer<String, Order> producer = new KafkaProducer<>(props);

        producer.send(new ProducerRecord<>(topic, order.getOrderId().toString(), order));
        producer.close();
    }
}
