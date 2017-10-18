package org.srcdeps.gradle.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.BuildException;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.BuildService;
import org.srcdeps.core.Gavtc;
import org.srcdeps.core.MavenLocalRepository;
import org.srcdeps.core.SrcVersion;
import org.srcdeps.core.config.BuilderIo;
import org.srcdeps.core.config.Configuration;
import org.srcdeps.core.config.ScmRepository;
import org.srcdeps.core.fs.BuildDirectoriesManager;
import org.srcdeps.core.fs.PathLock;
import org.srcdeps.core.shell.IoRedirects;

/**
 * See {@link #buildIfNecessary(String, String, String)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Named
@Singleton
public class SrcdepsService {
    private static final Logger log = LoggerFactory.getLogger(SrcdepsService.class);

    private static List<String> enhanceBuildArguments(List<String> buildArguments, Path configurationLocation,
            String localRepo) {
        List<String> result = new ArrayList<>();
        for (String arg : buildArguments) {
            if (arg.startsWith("-Dmaven.repo.local=")) {
                /* We won't touch maven.repo.local set in the user's config */
                log.debug("srcdeps: Forwards {} to the nested build as set in {}", arg, configurationLocation);
                return buildArguments;
            }
            result.add(arg);
        }

        String arg = "-Dmaven.repo.local=" + localRepo;
        log.debug("srcdeps: Forwards {} from the outer Maven build to the nested build", arg);
        result.add(arg);

        return Collections.unmodifiableList(result);
    }

    private final BuildDirectoriesManager buildDirectoriesManager;
    private final BuildService buildService;
    private final ConfigurationService configurationService;
    private final MavenLocalRepository localRepository;

    @Inject
    public SrcdepsService(ConfigurationService configurationService, BuildDirectoriesManager buildDirectoriesManager,
            BuildService buildService, MavenLocalRepository localRepository) {
        super();
        this.configurationService = configurationService;
        this.buildDirectoriesManager = buildDirectoriesManager;
        this.buildService = buildService;
        this.localRepository = localRepository;
    }

    /**
     * Finds the first {@link ScmRepository} associated with the given {@code groupId:artifactId:version} triple.
     *
     * @param repositories
     * @param groupId
     * @param artifactId
     * @param version
     * @return the matching {@link ScmRepository}
     */
    private static ScmRepository findScmRepo(List<ScmRepository> repositories, String groupId, String artifactId,
            String version) {
        for (ScmRepository scmRepository : repositories) {
            if (scmRepository.getGavSet().contains(groupId, artifactId, version)) {
                return scmRepository;
            }
        }
        throw new IllegalStateException(
                String.format("No srcdeps SCM repository configured in srcdeps.yaml for artifact [%s:%s:%s]", groupId,
                        artifactId, version));
    }

    /**
     * Builds the artifact given by the arguments if necessary and installs it to the Local Maven Repository.
     *
     * @param groupId
     * @param artifactId
     * @param version
     */
    public void buildIfNecessary(String groupId, String artifactId, String version) {
        final Configuration configuration = configurationService.getConfiguration();

        if (configuration.isSkip()) {
            log.info("srcdeps: Skipped");
        }

        Gavtc artifactGavtc = new Gavtc(groupId, artifactId, version, "jar"); // FIXME: "jar" should not be hard
                                                                              // coded but Gradle does not seem to have
                                                                              // a notion of type an classifier (does
                                                                              // it?)
        Path artfactPath = localRepository.resolve(artifactGavtc);
        if (!Files.exists(artfactPath)) {
            ScmRepository scmRepo = findScmRepo(configuration.getRepositories(), groupId, artifactId, version);
            SrcVersion srcVersion = SrcVersion.parse(version);
            try (PathLock projectBuildDir = buildDirectoriesManager.openBuildDirectory(scmRepo.getIdAsPath(),
                    srcVersion)) {

                /* query the delegate again, because things may have changed since we requested the lock */
                if (Files.exists(artfactPath)) {
                    log.debug("srcdeps: Found in the local repo and using it as is: {}", artfactPath);
                    return;
                } else {
                    /* no change in the local repo, let's build */
                    BuilderIo builderIo = scmRepo.getBuilderIo();
                    IoRedirects ioRedirects = IoRedirects.builder() //
                            .stdin(IoRedirects.parseUri(builderIo.getStdin())) //
                            .stdout(IoRedirects.parseUri(builderIo.getStdout())) //
                            .stderr(IoRedirects.parseUri(builderIo.getStderr())) //
                            .build();

                    List<String> buildArgs = enhanceBuildArguments(scmRepo.getBuildArguments(),
                            configurationService.getConfigurationLocation(),
                            localRepository.getRootDirectory().toString());

                    BuildRequest buildRequest = BuildRequest.builder() //
                            .dependentProjectRootDirectory(configurationService.getMultimoduleProjectRootDirectory())
                            .projectRootDirectory(projectBuildDir.getPath()) //
                            .scmUrls(scmRepo.getUrls()) //
                            .srcVersion(srcVersion) //
                            .buildArguments(buildArgs) //
                            .timeoutMs(scmRepo.getBuildTimeout().toMilliseconds()) //
                            .skipTests(scmRepo.isSkipTests()) //
                            .forwardProperties(configuration.getForwardProperties()) //
                            .addDefaultBuildArguments(scmRepo.isAddDefaultBuildArguments()) //
                            .verbosity(scmRepo.getVerbosity()) //
                            .ioRedirects(ioRedirects) //
                            .versionsMavenPluginVersion(scmRepo.getMaven().getVersionsMavenPluginVersion())
                            .gradleModelTransformer(scmRepo.getGradle().getModelTransformer()).build();
                    buildService.build(buildRequest);

                    /* check once again if the delegate sees the newly built artifact */
                    if (!Files.exists(artfactPath)) {
                        log.error(
                                "srcdeps: Build succeeded but the artifact {}:{}:{} is still not available in the local repository",
                                groupId, artifactId, version);
                    }
                }

            } catch (BuildException | IOException e) {
                log.error("srcdeps: Could not build {}:{}:{}" + groupId, artifactId, version, e);
            }
        }
    }
}
