package org.srcdeps.gradle.plugin;

import org.gradle.api.GradleException
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.RepositoryHandler;

/**
 * A Gradle plugin that scans given project's dependencies and in case a source dependency is found, it is built and
 * installed to Local Maven Repository unless it there already.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SrcdepsGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        project.afterEvaluate {
            org.srcdeps.gradle.plugin.Wiring.init(project);

            project.configurations.findAll { it.state != Configuration.State.UNRESOLVED }.each { configuration ->
                new SrcdepsResolver(configuration).resolveArtifacts()
            }
            def capturedProject = project
            project.configurations.findAll { it.state == Configuration.State.UNRESOLVED }.each { configuration ->
                configuration.incoming.beforeResolve {
                    new SrcdepsResolver(configuration).resolveArtifacts()
                }
            }
        }

    }

}
