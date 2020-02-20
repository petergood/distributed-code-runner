package pl.petergood.dcr.runnerworker.simple;

import com.google.common.io.Files;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import pl.petergood.dcr.acceptancetests.TestConsumerFactory;
import pl.petergood.dcr.acceptancetests.TestProducerFactory;
import pl.petergood.dcr.messaging.MessageConsumer;
import pl.petergood.dcr.messaging.MessageProducer;
import pl.petergood.dcr.messaging.schema.SimpleExecutionRequestMessage;
import pl.petergood.dcr.messaging.schema.SimpleExecutionResultMessage;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

@SpringBootTest
@ContextConfiguration(
        classes = SimpleRunnerWorkerApplication.class,
        initializers = E2ESimpleRunnerWorkerAcceptanceTest.ContextInitializer.class
)
@EmbeddedKafka(partitions = 1, topics = { "simple-execution-request", "simple-execution-result" }, controlledShutdown = true)
public class E2ESimpleRunnerWorkerAcceptanceTest {

    @Value("${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS +"}")
    private String bootstrapUrls;

    public static class ContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext,
                    "dcr.simplerunnerworker.broker.bootstrap.urls=${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS + "}",
                    "dcr.simplerunnerworker.jail.configuration.path=/test-nsjail.cfg",
                    "dcr.simplerunnerworker.jail.root.path=/");
        }
    }

    @Test
    public void verifyCodeIsExecuted() throws Exception {
        // given
        File binaryFile = new File("/dcr/dcracceptancetests/testbinaries/sum");
        byte[] bytes = Files.asByteSource(binaryFile).read();
        SimpleExecutionRequestMessage requestMessage = new SimpleExecutionRequestMessage("CPP", bytes, "121393 196418"); // 317811

        MessageProducer<SimpleExecutionRequestMessage> messageProducer = TestProducerFactory.createProducer(bootstrapUrls, "simple-execution-request");
        MessageConsumer<SimpleExecutionResultMessage> messageConsumer = TestConsumerFactory.createConsumer(SimpleExecutionResultMessage.class,
                bootstrapUrls, "test-simple-runner-worker", "simple-execution-result");
        Collection<SimpleExecutionResultMessage> receivedMessages = new LinkedBlockingDeque<>();
        messageConsumer.setOnMessageReceived(receivedMessages::addAll);

        Thread t = new Thread((Runnable) messageConsumer);
        t.start();

        // when
        messageProducer.publish(requestMessage);

        // then
        Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> receivedMessages.size() == 1);
        SimpleExecutionResultMessage message = receivedMessages.iterator().next();
        Assertions.assertThat(message.getExitCode()).isEqualTo(0);
        Assertions.assertThat(message.getStdout()).isEqualTo("317811");
        Assertions.assertThat(message.getStderr()).isEqualTo("");
    }

}
