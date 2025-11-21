package com.kafka.assignment;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public class OrderApiServer {

    public static void start() throws Exception {

        Server server = new Server(8080);

        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest req,
                               HttpServletResponse resp)
                    throws IOException {

                if (target.equals("/order") && req.getMethod().equals("POST")) {
                    String orderId = null;
                    String product = null;
                    String priceParam = null;

                    if (req.getContentType() != null && req.getContentType().contains("application/json")) {
                        StringBuilder jsonPayload = new StringBuilder();
                        try (BufferedReader reader = req.getReader()) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                jsonPayload.append(line);
                            }
                        }
                        String jsonString = jsonPayload.toString();
                        try {
                            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonString);
                            orderId = jsonObject.optString("orderId", null);
                            product = jsonObject.optString("product", null);
                            priceParam = jsonObject.optString("price", null);
                        } catch (org.json.JSONException e) {
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            resp.getWriter().println("Invalid JSON payload");
                            baseRequest.setHandled(true);
                            return;
                        }
                    } else {
                        orderId = req.getParameter("orderId");
                        product = req.getParameter("product");
                        priceParam = req.getParameter("price");
                    }

                    if (orderId == null || product == null || priceParam == null) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().println("Missing required parameters: orderId, product, or price");
                        baseRequest.setHandled(true);
                        return;
                    }

                    if (orderId.trim().isEmpty() || product.trim().isEmpty()) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().println("orderId and product cannot be empty");
                        baseRequest.setHandled(true);
                        return;
                    }

                    // Validate orderId
                    OrderValidator.ValidationResult validationResult = OrderValidator.validateOrderId(orderId);

                    try {
                        float price = Float.parseFloat(priceParam);
                        Order order = new Order(orderId, product, price);
                        OrderProducer orderProducer = new OrderProducer();

                        switch (validationResult) {
                            case DLQ:

                                orderProducer.sendToTopic(Config.get("topic.dlq"), order);
                                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
                                resp.getWriter().println("Order sent to DLQ.");
                                System.out.println("\nPERMANENT FAILURE in order :" + order);
                                System.out.println("Sending order to Dead Letter Queue (DLQ) for manual inspection...");
                                break;

                            case RETRY:

                                orderProducer.sendToTopic(Config.get("topic.retry"), order);
                                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
                                resp.getWriter().println("Retrying order...");
                                System.out.println("\nTemporary falier in order " + order);
                                System.out.println("RETRYING............");
                                break;

                            case VALID:

                                System.out.println("\n");
                                orderProducer.send(order);
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.getWriter().println("Order processed successfully " );
                                break;

                            case INVALID:
                                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                resp.getWriter().println("ERROR: Invalid orderId format. Must be a number.");
                                resp.getWriter().println("Order sent: " + order);
                        }
                    } catch (NumberFormatException e) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().println("Invalid price format");
                    }

                    baseRequest.setHandled(true);
                    return;
                }
            }
        });

        server.start();
        System.out.println("API server running → http://localhost:8080/order");
    }
}
