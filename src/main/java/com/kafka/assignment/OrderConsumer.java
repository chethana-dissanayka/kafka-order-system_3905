package com.kafka.assignment;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.TimeoutException;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * Kafka Consumer that:
 * 1. Reads Avro messages
 * 2. Calculates running average price
 * 3. Performs retry on temporary failures
 * 4. Sends permanently failed messages to DLQ
 */
public class OrderConsumer {

    private static float totalPrice = 0;
    private static int count = 0;
    private static int retryCount = 0;

    public static void main(String[] args) {

        // Consumer properties
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // Key = String
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");

        // Value = Avro
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);

        props.put("schema.registry.url", "http://localhost:8081");
        props.put("specific.avro.reader", "true");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, Order> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singleton("orders"));

        while (true) {
            ConsumerRecords<String, Order> records = consumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, Order> rec : records) {
                try {
                    Order order = rec.value();

                    // Simulated temporary failure → retry for order 5
                    // Check BEFORE processing to simulate temporary failure
                    if (order.getOrderId().toString().equals("5") && retryCount < 1) {
                        retryCount++;
                        throw new TimeoutException("Temporary error!");
                    }

                    // Real-time running average
                    totalPrice += order.getPrice();
                    count++;
                    float avg = totalPrice / count;

                    System.out.println("Consumed: " + order +
                            " | Running Avg Price = " + avg);

                    // Reset retry count after successful processing
                    retryCount = 0;

                } catch (TimeoutException e) {
                    System.out.println("Temporary error → Retrying...");
                    // retry on next poll
                } catch (Exception fatal) {
                    System.out.println("Permanent error → Sending to DLQ...");
                    sendToDLQ(rec);
                }
            }
        }
    }

    /**
     * Sends message to DLQ topic "orders-dlq"
     */
    private static void sendToDLQ(ConsumerRecord<String, Order> rec) {
        try {
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

            KafkaProducer<String, String> producer = new KafkaProducer<>(props);

            producer.send(new ProducerRecord<>(
                    "orders-dlq",
                    rec.key(),
                    rec.value().toString()
            ));

            producer.close();

        } catch (Exception ex) {
            System.err.println("Error sending to DLQ: " + ex.getMessage());
        }
    }
}
