package com.kafka.assignment;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real-time aggregation of order prices.
 * Calculates running average of all processed orders.
 */
public class PriceAggregator {

    private static final AtomicInteger orderCount = new AtomicInteger(0);
    private static final AtomicReference<Double> totalPrice = new AtomicReference<>(0.0);
    private static final AtomicReference<Double> runningAverage = new AtomicReference<>(0.0);

    /**
     * Add a new order price to the aggregation
     * @param price The price of the order
     */
    public static void addPrice(float price) {
        int count = orderCount.incrementAndGet();
        double newTotal = totalPrice.accumulateAndGet((double) price, Double::sum);
        double newAverage = newTotal / count;
        runningAverage.set(newAverage);

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         REAL-TIME PRICE AGGREGATION                      ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf("║  Current Order Price:  $%.2f%n", price);
        System.out.printf("║  Total Orders Processed: %d%n", count);
        System.out.printf("║  Total Revenue:        $%.2f%n", newTotal);
        System.out.printf("║  Running Average Price: $%.2f%n", newAverage);
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    /**
     * Get the current running average of all prices
     * @return The running average
     */
    public static double getRunningAverage() {
        return runningAverage.get();
    }

    /**
     * Get the total number of orders processed
     * @return Total order count
     */
    public static int getOrderCount() {
        return orderCount.get();
    }

    /**
     * Get the total price sum
     * @return Total price
     */
    public static double getTotalPrice() {
        return totalPrice.get();
    }

    /**
     * Reset aggregation (useful for testing)
     */
    public static void reset() {
        orderCount.set(0);
        totalPrice.set(0.0);
        runningAverage.set(0.0);
    }
}

