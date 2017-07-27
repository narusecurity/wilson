package com.me2ds.wilson;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import static com.me2ds.wilson.spring.SpringExtension.SpringExtProvider;

/**
 * The application configuration.
 * <p>
 * 2015.05.05:
 * Note that @RabbitListener is fully supported by /META-INF/rabbit-context.xml and, for that, no explicit bean
 * methods are needed at this point.
 */
@Configuration
@Import(RabbitConfiguration.class)
class SpringConfiguration {

    // the application context is needed to initialize the Akka Spring Extension
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Actor system singleton for this application.
     */
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public ActorSystem actorSystem(Config config) {
        ActorSystem system = ActorSystem.create("Wilson", config);
        // initialize the application context in the Akka Spring Extension
        SpringExtProvider.get(system).initialize(applicationContext);
        return system;
    }
}