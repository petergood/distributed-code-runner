package pl.petergood.dcr.jail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NsJailConfig {

    private Map<String, List<String>> flags = new HashMap<>();
    private File hostJailPath;
    private File absoluteJailPath;

    private NsJailConfig() {
    }

    public String getCommandFlags() {
        StringBuilder commandFlags = new StringBuilder();
        List<String> configLocations = flags.get("config");

        if (configLocations != null) {
            commandFlags.append(String.format("--config %s ", configLocations.get(configLocations.size() - 1)));
        }

        flags.entrySet().stream()
            .filter((entry) -> !entry.getKey().equals("config"))
            .forEach((entry) ->
                entry.getValue().forEach((flagValue) -> commandFlags.append(String.format("--%s %s ", entry.getKey(), flagValue)))
            );
        return commandFlags.toString().substring(0, commandFlags.length() - 1);
    }

    public File getHostJailPath() {
        return hostJailPath;
    }

    public File getAbsoluteJailPath() {
        return absoluteJailPath;
    }

    public static class Builder {
        private Map<String, List<String>> flags = new HashMap<>();
        private File hostJailPath;
        private String jailDirectory;
        private NsJailDirectoryMode jailDirectoryMode;

        public Builder setConfig(String path) {
            addFlag("config", path);
            return this;
        }

        public Builder setLogFile(String path) {
            addFlag("log", path);
            return this;
        }

        public Builder readOnlyMount(String path) {
            addFlag("bindmount_ro", path);
            return this;
        }

        public Builder readWriteMount(String path) {
            addFlag("bindmount", path);
            return this;
        }

        public Builder workingDirectory(String path) {
            addFlag("cwd", path);
            return this;
        }

        public Builder setHostJailPath(File path) {
            this.hostJailPath = path;
            return this;
        }

        public Builder setJailDirectoryName(String directory, NsJailDirectoryMode jailDirectoryMode) {
            this.jailDirectory = directory;
            this.jailDirectoryMode = jailDirectoryMode;
            return this;
        }

        private void addFlag(String flag, String value) {
            flags.putIfAbsent(flag, new ArrayList<>());
            flags.get(flag).add(value);
        }

        public NsJailConfig build() {
            NsJailConfig jailConfig = new NsJailConfig();

            if (hostJailPath == null || jailDirectory == null) {
                throw new IllegalStateException("Host jail path and relative jail path must be set!");
            }

            File absoluteJailPath = new File(hostJailPath, jailDirectory);

            if (jailDirectoryMode == NsJailDirectoryMode.READ_WRITE) {
                readWriteMount(absoluteJailPath.getAbsolutePath());
            } else {
                readOnlyMount(absoluteJailPath.getAbsolutePath());
            }

            workingDirectory(absoluteJailPath.getAbsolutePath());

            jailConfig.flags = this.flags;
            jailConfig.hostJailPath = this.hostJailPath;
            jailConfig.absoluteJailPath = absoluteJailPath;
            return jailConfig;
        }
    }

}
