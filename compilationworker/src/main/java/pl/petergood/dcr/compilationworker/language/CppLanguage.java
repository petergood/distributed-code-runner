package pl.petergood.dcr.compilationworker.language;

import pl.petergood.dcr.compilationworker.source.ProgramSource;
import pl.petergood.dcr.jail.Jail;
import pl.petergood.dcr.shell.ExecutionResult;

import java.io.File;

public class CppLanguage implements Language {

    private Jail jail;

    public CppLanguage(Jail jail) {
        this.jail = jail;
    }

    @Override
    public ProcessingResult process(ProgramSource programSource) {
        ExecutionResult executionResult = jail.executeInJail(new String[] {
                "g++",
                programSource.getFile().getAbsolutePath(),
                "-o",
                "output"
        });

        return new ProcessingResult(new File(programSource.getFile().getParentFile(), "output"), executionResult);
    }
}
