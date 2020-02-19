package pl.petergood.dcr.runnerworker.core.strategy;

import com.google.common.io.Files;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.petergood.dcr.acceptancetests.AcceptanceTestsJailFactory;
import pl.petergood.dcr.file.FileInteractor;
import pl.petergood.dcr.file.FileSystemFileInteractor;
import pl.petergood.dcr.jail.Jail;
import pl.petergood.dcr.jail.NsJailDirectoryMode;
import pl.petergood.dcr.messaging.schema.SimpleExecutionRequestMessage;
import pl.petergood.dcr.messaging.schema.SimpleExecutionResultMessage;

import java.io.File;

public class SimpleExecutionStrategyAcceptanceTest {

    @Test
    public void verifyExecutableIsExecuted() throws Exception {
        // given
        byte[] bytes = Files.asByteSource(new File("/dcr/dcracceptancetests/testbinaries/sum")).read();
        SimpleExecutionRequestMessage requestMessage = new SimpleExecutionRequestMessage("CPP", bytes, "2584 4181"); // 6765
        Jail jail = AcceptanceTestsJailFactory.getJail(NsJailDirectoryMode.READ_ONLY);
        FileInteractor fileInteractor = new FileSystemFileInteractor();
        SimpleExecutionStrategy strategy = new SimpleExecutionStrategy(jail, fileInteractor);

        // when
        SimpleExecutionResultMessage resultMessage = strategy.execute(requestMessage);

        // then
        Assertions.assertThat(resultMessage.getExitCode()).isEqualTo(0);
        Assertions.assertThat(resultMessage.getStdout()).isEqualTo("6765");
        Assertions.assertThat(resultMessage.getStderr()).isEqualTo("");
    }

}
