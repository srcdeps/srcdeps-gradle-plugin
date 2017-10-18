package org.srcdeps.gradle.plugin.quickstarts.mvn.git.revision;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.gradle.plugin.quickstarts.mvn.git.revision.AppClient;

public class AppClientTest {

    @Test
    public void test() {
        Assert.assertEquals("Hello World!", new AppClient().getApp().getMessage());
    }

}
