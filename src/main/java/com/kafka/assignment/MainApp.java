package com.kafka.assignment;

public class MainApp {
    public static void main(String[] args) throws Exception {

        // Start REST API
        OrderApiServer.start();

        // Start consumer (on separate thread)
        new Thread(() -> OrderConsumer.main(null)).start();

        System.out.println("System ready.");
    }
}
