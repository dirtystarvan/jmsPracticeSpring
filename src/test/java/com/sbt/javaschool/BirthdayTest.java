package com.sbt.javaschool;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.util.Enumeration;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JmsConfiguration.class)
public class BirthdayTest {
    private static final String[] WISHES = {"Счастья!", "Здоровья!", "Удачи!", "Успехов!", "Улыбок!"};
    private static final String[] QUEUES = {"DIMA.QUEUE.ac1d", "SASHA.QUEUE.ac1d", "MASHA.QUEUE.ac1d", "IVAN.QUEUE.ac1d", "TAMARA.QUEUE.ac1d"};

    @Autowired
    JmsTemplate jmsTemplate;

    String wish;
    String id;
    String queue;

    boolean wishOk;
    boolean queueOk;
    boolean replyToQOk;

    @Before
    public void init() {
        wish = WISHES[new Random().nextInt(WISHES.length - 1)];
        queue = QUEUES[new Random().nextInt(QUEUES.length - 1)];
        id = UUID.randomUUID().toString();
        jmsTemplate.send("WISH.QUEUE.ac1d", session -> {
            TextMessage message = session.createTextMessage("Пустое пожелание");
            message.setStringProperty("DestinationQueue", "FAKE.QUEUE.ac1d");
            return message;
        });
        jmsTemplate.send("WISH.QUEUE.ac1d", session -> {
            Message message = session.createTextMessage(wish);
            message.setStringProperty("WishID", id);
            message.setStringProperty("DestinationQueue", queue);
            return message;
        });
    }

    @Test
    public void positiveBirthdayScenario() throws JMSException {
        AtomicReference<Message> sentMessage = new AtomicReference<>();
        jmsTemplate.send("WISH.TOPIC.ac1d", session -> {
            Message message = session.createTextMessage(wish);
            message.setStringProperty("WishID", id);
            sentMessage.set(message);
            return message;
        });
        Message answer = jmsTemplate.receiveSelected("ANSWER.QUEUE.ac1d", "JMSCorrelationID='" + sentMessage.get().getJMSMessageID() + "'");
        Assert.assertNotNull("Ответ на поздравление не получен", answer);
        Assert.assertTrue("Отправлено неправильное пожелание", wishOk);
        Assert.assertTrue("Пожелание отправлено не в ту очередь", queueOk);
        Assert.assertTrue("У пожеления не заполнен заголовок ReplyTo", replyToQOk);
    }

    @JmsListener(destination = "DIMA.QUEUE.ac1d")
    @JmsListener(destination = "SASHA.QUEUE.ac1d")
    @JmsListener(destination = "MASHA.QUEUE.ac1d")
    @JmsListener(destination = "IVAN.QUEUE.ac1d")
    @JmsListener(destination = "TAMARA.QUEUE.ac1d")
    public void handleInboundMessage(Message message) throws JMSException {
        wishOk = wish.equals(message.getBody(String.class));
        queueOk = queue.equals(((Queue) message.getJMSDestination()).getQueueName());
        replyToQOk = message.getJMSReplyTo() != null;
        jmsTemplate.send(message.getJMSReplyTo(), session -> {
            TextMessage textMessage = session.createTextMessage("Спасибо!");
            Enumeration propertyNames = message.getPropertyNames();
            while (propertyNames.hasMoreElements()) {
                String key = String.valueOf(propertyNames.nextElement());
                textMessage.setObjectProperty(key, message.getObjectProperty(key));
            }
            return textMessage;
        });
    }
}
