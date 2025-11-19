package com.kafka.assignment;

import org.apache.kafka.clients.producer.*;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

import java.util.Properties;
import java.util.Random;

public class OrderProducer {

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // Key = String
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");

        // Value = Avro
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaAvroSerializer.class);

        props.put("schema.registry.url", "http://localhost:8081");

        // Reliability configs
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 5);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        KafkaProducer<String, Order> producer = new KafkaProducer<>(props);

        Random random = new Random();

        for (int i = 1; i <= 20; i++) {

            Order order = new Order(
                    String.valueOf(i),
                    "Item" + i,
                    random.nextFloat(100)
            );

            ProducerRecord<String, Order> record =
                    new ProducerRecord<>("orders", order.getOrderId().toString(), order);

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("Produced → " + order);
                } else {
                    System.err.println("Error: " + exception.getMessage());
                }
            });

            Thread.sleep(500);
        }

        producer.close();
    }
}
