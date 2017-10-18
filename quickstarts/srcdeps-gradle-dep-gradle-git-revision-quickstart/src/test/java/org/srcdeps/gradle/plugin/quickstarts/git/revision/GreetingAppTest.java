package org.srcdeps.gradle.plugin.quickstarts.git.revision;

import org.junit.Assert;
import org.junit.Test;

public class GreetingAppTest {

    @Test
    public void test() {
        Assert.assertEquals("Hello World!", new GreetingApp().getGreeter().greet("World"));
    }

}
