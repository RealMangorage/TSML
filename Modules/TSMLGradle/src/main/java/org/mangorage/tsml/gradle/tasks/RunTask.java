package org.mangorage.tsml.gradle.tasks;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.JavaExec;

import javax.inject.Inject;

public abstract class RunTask extends JavaExec {
    @Inject
    public RunTask() {
        setGroup("tsml");
        setDescription("Runs the bot");

        setWorkingDir(getProject().file("build/run/"));

        // Create your module path from the config
        FileCollection classPath = getProject().getTasksByName("jarJar", false)
                .stream()
                .findFirst()
                .orElseThrow()
                .getOutputs()
                .getFiles();

        setClasspath(classPath); // EMPTY CLASSPATH, this is MODULE mode
        getMainClass().set("org.mangorage.tsml.bootstrap.Bootstrap");
    }
}
