package org.srcdeps.gradle.plugin.quickstarts.git.revision;

public class GreetingApp {
    private final org.srcdeps.test.gradle.api.Greeter greeter = new org.srcdeps.test.gradle.impl.HelloGreeter();

    public org.srcdeps.test.gradle.api.Greeter getGreeter() {
        return greeter;
    }
}
