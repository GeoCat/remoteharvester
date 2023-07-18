package com.geocat.ingester.config;

import com.geocat.ingester.dblogging.MYUnitOfWorkFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.connection.JmsTransactionManager;


@Configuration
public class Config {
    //String brokerUrl = "tcp://localhost:61616?jms.prefetchPolicy.queuePrefetch=1";
    @Value("${activemq.url}")
    String brokerUrl;

    @Autowired
    CamelContext camelContext;

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(brokerUrl);
        org.apache.activemq.RedeliveryPolicy redeliveryPolicy = factory.getRedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(3);
        redeliveryPolicy.setRedeliveryDelay(3000);
        redeliveryPolicy.setInitialRedeliveryDelay(3000);
        redeliveryPolicy.setMaximumRedeliveryDelay(5000);
        return factory;
    }

    @Bean
    public CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.setUseMDCLogging(true);
                context.adapt(ExtendedCamelContext.class).setUnitOfWorkFactory(new MYUnitOfWorkFactory());
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
            }
        };
    }

    @Bean
    public ActiveMQComponent activemq(ActiveMQConnectionFactory connectionFactory) {

        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory(connectionFactory);
        pooledConnectionFactory.setMaxConnections(50);

        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
        jmsTransactionManager.setConnectionFactory(pooledConnectionFactory);

        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setConnectionFactory(pooledConnectionFactory);
        activeMQComponent.setTransacted(true);
        activeMQComponent.setTransactedInOut(true);
        activeMQComponent.setTransactionManager(jmsTransactionManager);

        //  activeMQComponent.setLazyCreateTransactionManager (false);

        activeMQComponent.setCacheLevelName("CACHE_NONE");
        activeMQComponent.setAcknowledgementModeName("SESSION_TRANSACTED");


        return activeMQComponent;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propsConfig
                = new PropertySourcesPlaceholderConfigurer();
        propsConfig.setLocation(new ClassPathResource("git.properties"));
        propsConfig.setIgnoreResourceNotFound(true);
        propsConfig.setIgnoreUnresolvablePlaceholders(true);
        return propsConfig;
    }
}
