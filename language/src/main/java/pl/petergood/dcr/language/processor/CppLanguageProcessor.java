package pl.petergood.dcr.language.processor;

import pl.petergood.dcr.jail.Jail;
import pl.petergood.dcr.jail.JailedFile;
import pl.petergood.dcr.language.source.ProgramSource;
import pl.petergood.dcr.shell.ExecutionResult;

public class CppLanguageProcessor implements LanguageProcessor {

    private Jail jail;

    public CppLanguageProcessor(Jail jail) {
        this.jail = jail;
    }

    @Override
    public ProcessingResult process(ProgramSource programSource) {
        ExecutionResult executionResult = jail.executeAndReturnOutputContent(new String[] {
                "/usr/bin/g++",
                programSource.getJailedFile().getAbsolutePath(),
                "-o",
                "output"
        });

        return new ProcessingResult(new JailedFile(jail.getAbsoluteJailPath(), "output", jail), executionResult);
    }
}
