package com.sbt.javaschool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Configuration
@ComponentScan
@EnableJms
public class JmsConfiguration {
    @Autowired
    JmsTemplate jmsTemplate;

    @Bean
    public InitialContext getInitialContext() throws NamingException {
        return new InitialContext();
    }

    @Bean
    public ConnectionFactory connectionFactory(InitialContext context) throws NamingException {
        ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("ConnectionFactory");
        SingleConnectionFactory singleConnectionFactory = new SingleConnectionFactory(connectionFactory);
        singleConnectionFactory.setReconnectOnException(true);
        return connectionFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setReceiveTimeout(2000L);
        template.setPubSubDomain(true);
        return jmsTemplate;
    }

    @Bean
//    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory (ConnectionFactory connectionFactory, MessageConverter messageConverter) {
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory (ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        return factory;
    }

    @JmsListener(destination = "TEST.TOPIC")
    public void handleMessage(Message message) throws JMSException {
        System.out.println("AnnotationListener" + message.getJMSMessageID());
    }
}
