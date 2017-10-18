package org.srcdeps.gradle.plugin;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.config.yaml.YamlConfigurationIo;
import org.srcdeps.core.config.Configuration;
import org.srcdeps.core.config.ConfigurationException;
import org.srcdeps.core.config.tree.walk.DefaultsAndInheritanceVisitor;
import org.srcdeps.core.config.tree.walk.OverrideVisitor;

/**
 * Provides the srcdeps {@link Configuration} and paths to some project directories.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Named
@Singleton
public class ConfigurationService {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationService.class);
    public static final String SRCDEPS_YAML_PATH = "SRCDEPS_YAML_PATH";
    private final Configuration configuration;

    private final Path configurationLocation;
    private final Path multimoduleProjectRootDirectory;

    @Inject
    public ConfigurationService(@Named(SRCDEPS_YAML_PATH) Path srcdepsYamlPath) {
        super();
        this.configurationLocation = srcdepsYamlPath;
        this.multimoduleProjectRootDirectory = srcdepsYamlPath.getParent();

        final Configuration.Builder configBuilder;
        if (Files.exists(srcdepsYamlPath)) {
            log.debug("srcdeps: Using configuration {}", srcdepsYamlPath);
            final String encoding = System.getProperty(Configuration.getSrcdepsEncodingProperty(), "utf-8");
            final Charset cs = Charset.forName(encoding);
            try (Reader r = Files.newBufferedReader(srcdepsYamlPath, cs)) {
                configBuilder = new YamlConfigurationIo().read(r);
            } catch (IOException | ConfigurationException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.warn("srcdeps: Could not locate srcdeps configuration at {}, defaulting to an empty configuration",
                    srcdepsYamlPath);
            configBuilder = Configuration.builder();
        }

        this.configuration = configBuilder //
                .accept(new OverrideVisitor(System.getProperties())) //
                .accept(new DefaultsAndInheritanceVisitor()) //
                .build();
    }

    /**
     * @return the {@link Configuration} loaded fron {@link #getConfigurationLocation()}.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * @return the {@link Path} to {@code srcdeps.yaml}
     */
    public Path getConfigurationLocation() {
        return configurationLocation;
    }

    /**
     * @return the {@link Path} to the root directory of the current Gradle multimodule project tree
     */
    public Path getMultimoduleProjectRootDirectory() {
        return multimoduleProjectRootDirectory;
    }

}
