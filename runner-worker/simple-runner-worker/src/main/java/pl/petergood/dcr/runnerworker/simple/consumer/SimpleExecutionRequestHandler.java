package pl.petergood.dcr.runnerworker.simple.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.petergood.dcr.configurationservice.client.ConfigurationServiceClient;
import pl.petergood.dcr.configurationservice.client.ExecutionProfile;
import pl.petergood.dcr.configurationservice.client.ExecutionProfileNotFoundException;
import pl.petergood.dcr.file.FileInteractor;
import pl.petergood.dcr.jail.Jail;
import pl.petergood.dcr.jail.JailFactory;
import pl.petergood.dcr.jail.JailDirectoryMode;
import pl.petergood.dcr.jail.NsJailException;
import pl.petergood.dcr.messaging.MessageProducer;
import pl.petergood.dcr.messaging.MessageReceivedEventHandler;
import pl.petergood.dcr.messaging.schema.SimpleExecutionRequestMessage;
import pl.petergood.dcr.messaging.schema.SimpleExecutionResultMessage;
import pl.petergood.dcr.runnerworker.core.strategy.SimpleExecutionStrategy;
import pl.petergood.dcr.runnerworker.simple.configuration.JailConfiguration;
import pl.petergood.dcr.runnerworker.simple.producer.MessageProducerConfiguration;
import pl.petergood.dcr.shell.TerminalInteractor;

import java.io.IOException;
import java.util.List;

public class SimpleExecutionRequestHandler implements MessageReceivedEventHandler<SimpleExecutionRequestMessage> {

    private TerminalInteractor terminalInteractor;
    private FileInteractor fileInteractor;
    private JailConfiguration jailConfiguration;
    private ConfigurationServiceClient configurationServiceClient;
    private MessageProducer<SimpleExecutionResultMessage> executionResultMessageProducer;

    private Logger LOG = LoggerFactory.getLogger(SimpleExecutionRequestHandler.class);

    public SimpleExecutionRequestHandler(TerminalInteractor terminalInteractor,
                                         FileInteractor fileInteractor,
                                         JailConfiguration jailConfiguration,
                                         ConfigurationServiceClient configurationServiceClient,
                                         MessageProducerConfiguration messageProducerConfiguration) {
        this.terminalInteractor = terminalInteractor;
        this.fileInteractor = fileInteractor;
        this.jailConfiguration = jailConfiguration;
        this.configurationServiceClient = configurationServiceClient;
        this.executionResultMessageProducer = messageProducerConfiguration.getResultMessageProducer();
    }

    @Override
    public void handleMessageBatch(List<SimpleExecutionRequestMessage> messages) {
        messages.forEach(this::handleMessage);
    }

    private void handleMessage(SimpleExecutionRequestMessage message) {
        try {
            ExecutionProfile executionProfile = configurationServiceClient.getExecutionProfile(message.getExecutionProfileId());

            Jail jail = JailFactory.createJail(jailConfiguration.getJailRootPath(), jailConfiguration.getJailConfigurationPath(),
                    terminalInteractor, JailDirectoryMode.READ_ONLY, executionProfile.getCpuTimeLimitSeconds(), executionProfile.getMemoryLimitBytes());
            SimpleExecutionStrategy executionStrategy = new SimpleExecutionStrategy(jail, fileInteractor);

            SimpleExecutionResultMessage resultMessage = executionStrategy.execute(message);
            executionResultMessageProducer.publish(resultMessage);
        } catch (NsJailException | IOException ex) {
            LOG.error(ex.getMessage());
            ex.printStackTrace();
            // TODO: send error message
        } catch (ExecutionProfileNotFoundException ex) {
            LOG.error("Execution profile with id={} not found", message.getExecutionProfileId());
            // TODO: send error message/use default execution profile
        }
    }
}
