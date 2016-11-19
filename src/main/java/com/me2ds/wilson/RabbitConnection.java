package com.me2ds.wilson;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.typesafe.config.Config;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by w3kim on 2016-11-18.
 */
public class RabbitConnection {
    private static Connection conn;

    public static synchronized Connection getConnection() {
        if (conn == null) {
            try {
                Config rc = Wilson.getRabbitConfig();

                ConnectionFactory factory = new ConnectionFactory();
                factory.setUsername(rc.getString("username"));
                factory.setPassword(rc.getString("password"));
                factory.setVirtualHost(rc.getString("virtualhost"));
                factory.setHost(rc.getString("host"));
                factory.setPort(rc.getInt("port"));

                conn = factory.newConnection();
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        return conn;
    }
}
