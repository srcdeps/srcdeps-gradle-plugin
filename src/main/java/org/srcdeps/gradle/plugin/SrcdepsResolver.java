package org.srcdeps.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.SrcVersion;

/**
 * See {@link #resolveArtifacts()}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SrcdepsResolver {
    private static class DependencyAction implements Action<DependencyResolveDetails> {

        @Override
        public void execute(DependencyResolveDetails dep) {
            ModuleVersionSelector requested = dep.getRequested();
            final String version = requested.getVersion();
            if (SrcVersion.isSrcVersion(version)) {
                final SrcdepsService srcdepsService = Wiring.getInjector().getInstance(SrcdepsService.class);
                srcdepsService.buildIfNecessary(requested.getGroup(), requested.getName(), version);
            }
        }

    }

    private static final Logger log = LoggerFactory.getLogger(SrcdepsResolver.class);

    private final Configuration configuration;
    private final Action<DependencyResolveDetails> dependencyAction = new DependencyAction();

    public SrcdepsResolver(Configuration configuration) {
        super();
        this.configuration = configuration;
    }

    /**
     * Walks through the Gradle {@link Configuration} and initiates the build of source dependencies
     */
    public void resolveArtifacts() {
        if (configuration.isCanBeResolved()) {
            configuration.resolutionStrategy(new Action<ResolutionStrategy>() {
                @Override
                public void execute(ResolutionStrategy strategy) {
                    strategy.eachDependency(dependencyAction);
                }
            });
        } else {
            log.warn("srcdeps: Configuration {} cannot be resolved", configuration);
        }

    }

}
