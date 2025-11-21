package com.kafka.assignment;

import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static Properties props = new Properties();

    static {
        try (InputStream input =
                     Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load config.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
