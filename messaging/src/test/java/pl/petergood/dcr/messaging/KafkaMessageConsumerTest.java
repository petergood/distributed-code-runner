package pl.petergood.dcr.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;

import java.time.Duration;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class KafkaMessageConsumerTest {

    @Rule
    public EmbeddedKafkaRule kafkaRule = new EmbeddedKafkaRule(1, false, "test-topic");

    private Producer<String, String> producer;

    @Before
    public void setupProducer() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaRule.getEmbeddedKafka().getBrokersAsString());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(properties);
    }

    @Test
    public void verifyConsumerGetsMessages() {
        // given
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaRule.getEmbeddedKafka().getBrokersAsString());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        MessageConsumer<String, String> messageConsumer = new KafkaMessageConsumer<>(properties, "test-topic", Duration.ofSeconds(1),
                new StringDeserializer(), new StringDeserializer());
        Collection<String> receivedMessages = new LinkedBlockingDeque<>();

        messageConsumer.setOnMessageReceived((messages) -> receivedMessages.addAll(messages.stream().map(Message::getMessage).collect(Collectors.toList())));

        // when
        Thread t = new Thread((Runnable) messageConsumer);
        t.start();

        producer.send(new ProducerRecord<>("test-topic", "hello world!"));

        // then
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> receivedMessages.size() == 1);
        Assertions.assertThat(receivedMessages.size()).isEqualTo(1);
        Assertions.assertThat(receivedMessages.contains("hello world!")).isTrue();
    }

    @Test
    public void verifyMessageHandlerIsOnlyCalledWhenNewMessagesArrive() throws Exception {
        // given
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaRule.getEmbeddedKafka().getBrokersAsString());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        MessageConsumer<String, String> messageConsumer = new KafkaMessageConsumer<>(properties, "test-topic", Duration.ofSeconds(1),
                new StringDeserializer(), new StringDeserializer());

        AtomicInteger atomicInteger = new AtomicInteger();
        messageConsumer.setOnMessageReceived((messages) -> atomicInteger.incrementAndGet());
        Thread t = new Thread((Runnable) messageConsumer);
        t.start();

        // when
        producer.send(new ProducerRecord<>("test-topic", "message"));
        Thread.sleep(5000);

        // then
        Assertions.assertThat(atomicInteger.get()).isEqualTo(1);
    }

}
