package com.me2ds.wilson;

import com.typesafe.config.Config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by w3kim on 15-07-12.
 */
@Configuration
public class RabbitConfiguration {

    @Bean
    public ConnectionFactory connectionFactory() {
        Config mc = Wilson.getRabbitConfig();
        CachingConnectionFactory cf =
                new CachingConnectionFactory(
                        mc.getString("host"),
                        mc.getInt("port")
                );
        cf.setRequestedHeartBeat(3); // check RabbitMQ status
        cf.setUsername(mc.getString("username"));
        cf.setPassword(mc.getString("password"));
        return cf;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean(name = "outputExchange")
    public TopicExchange outputExchange() {
        Config conf = Wilson.getRabbitConfig();
        return new TopicExchange(
                conf.getString("exchange"),
                false,
                false
        );
    }

    @Bean(name = "outputQueue")
    public Queue outputQueue() {
        Config conf = Wilson.getRabbitConfig();
        return new Queue(
                conf.getString("queue"),
                false
        );
    }

    @Bean(name = "outputBinding")
    public Binding outputBinding() {
        Config conf = Wilson.getRabbitConfig();
        return BindingBuilder
                .bind(outputQueue())
                .to(outputExchange())
                .with(conf.getString("routingkey"));
    }
}