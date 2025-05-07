package io.wadoon.keyci;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class KeYPlugin implements Plugin<Project> {
    public void apply(Project target) {
        target.getTasks().register("keyverify", KeYVerifyTask.class);
        target.getConfigurations().create("key");
        // greetingFile.set(layout.buildDirectory.file("hello.txt"))
    }
}
