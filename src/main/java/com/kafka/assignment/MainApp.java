package com.kafka.assignment;

public class MainApp {
    public static void main(String[] args) throws Exception {

        OrderApiServer.start();
        new Thread(() -> OrderConsumer.main(null)).start();

        System.out.println("System ready.");
    }
}
