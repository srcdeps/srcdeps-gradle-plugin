package org.srcdeps.gradle.plugin.itest;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.srcdeps.core.Gavtc;
import org.srcdeps.core.MavenLocalRepository;
import org.srcdeps.core.util.SrcdepsCoreUtils;

public class SrcdepsGradlePluginTest {
    private static final MavenLocalRepository mvnLocalRepo = MavenLocalRepository.autodetect();

    private static final String ORG_SRCDEPS_ITEST_GRADLE_GROUPID = "org.srcdeps.test.gradle";
    private static final Path projectBuilDir = Paths
            .get(System.getProperty("project.build.directory", new File("build").getAbsolutePath()));
    protected static final String QUICKSTART_GROUPID = "org.srcdeps.gradle.plugin.quickstarts";
    private static final String QUICKSTART_VERSION = "0.0.1-SNAPSHOT";

    private static final Path quickstartsDir;
    private static final Path testProjectsDir;

    static {
        testProjectsDir = projectBuilDir.resolve("test-projects");
        quickstartsDir = projectBuilDir.getParent().resolve("quickstarts");
    }

    public static void assertExists(Path path) {
        Assert.assertTrue(String.format("File or directory does not exist [%s]", path.toString()), Files.exists(path));
    }

    protected static String groupId(String project) {
        return QUICKSTART_GROUPID + "." + project;
    }

    protected static String pom(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version + ":pom";
    }

    protected static String pomJar(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version + ":[pom,jar]";
    }

    @Rule
    public TestName name = new TestName();

    private Path testProjectRootDir;

    private void assertBuild(String project, String[] expectedGavtcs) throws IOException {
        SrcdepsCoreUtils.deleteDirectory(testProjectRootDir);
        SrcdepsCoreUtils.copyDirectory(quickstartsDir.resolve(project), testProjectRootDir);
        List<Path> expectedToExist = new ArrayList<>();
        for (String gavtcPattern : expectedGavtcs) {
            for (Gavtc gavtc : Gavtc.ofPattern(gavtcPattern)) {
                SrcdepsCoreUtils.deleteDirectory(mvnLocalRepo.resolveGroup(gavtc.getGroupId()));
                expectedToExist.add(mvnLocalRepo.resolve(gavtc));
            }
        }

        BuildResult result = GradleRunner.create() //
                .withProjectDir(testProjectRootDir.toFile()) //
                .withArguments("clean", "build", "install") //
                .build();

        Assert.assertEquals(result.task(":test").getOutcome(), SUCCESS);

        for (Path p : expectedToExist) {
            assertExists(p);
        }
    }

    @Test
    public void depGradleGitRevision() throws IOException {

        final String project = "srcdeps-gradle-dep-gradle-git-revision-quickstart";
        final String srcVersion = "1.0-SRC-revision-e63539236a94e8f6c2d720f8bda0323d1ce4db0f";

        String[] expectedGavtcs = new String[] { //
                pomJar(groupId(project), project, QUICKSTART_VERSION), //
                pomJar(ORG_SRCDEPS_ITEST_GRADLE_GROUPID, "srcdeps-test-artifact-gradle-api", srcVersion), //
                pomJar(ORG_SRCDEPS_ITEST_GRADLE_GROUPID, "srcdeps-test-artifact-gradle-impl", srcVersion) //
        };

        assertBuild(project, expectedGavtcs);

    }
    protected static final String ORG_L2X6_MAVEN_SRCDEPS_ITEST_GROUPID = "org.l2x6.maven.srcdeps.itest";

    @Test
    public void depMavenGitRevision() throws IOException {

        final String project = "srcdeps-gradle-dep-maven-git-revision-quickstart";
        final String srcVersion = "0.0.1-SRC-revision-66ea95d890531f4eaaa5aa04a9b1c69b409dcd0b";

        String[] expectedGavtcs = new String[] { //
                pom(groupId(project), project, QUICKSTART_VERSION), //
                pomJar(ORG_L2X6_MAVEN_SRCDEPS_ITEST_GROUPID, "srcdeps-test-artifact", srcVersion) //
        };

        assertBuild(project, expectedGavtcs);

    }

    @Before
    public void setup() throws IOException {
        testProjectRootDir = testProjectsDir.resolve(name.getMethodName());
        SrcdepsCoreUtils.ensureDirectoryExists(testProjectRootDir);

    }

}
