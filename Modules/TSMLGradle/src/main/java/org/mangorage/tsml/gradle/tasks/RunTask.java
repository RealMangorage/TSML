package org.mangorage.tsml.gradle.tasks;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.JavaExec;
import org.mangorage.tsml.gradle.Helper;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public abstract class RunTask extends JavaExec {
    @Inject
    public RunTask(RunType type) throws IOException {
        setGroup("tsml");
        setDescription("Runs the bot");

        setWorkingDir(getProject().file("build/run/" + type.name().toLowerCase() + "/"));

        if (!getWorkingDir().exists()) {
            Files.createDirectories(getWorkingDir().toPath());
        }

        // Create your module path from the config
        FileCollection classPath = getProject().getTasksByName("jarJar", false)
                .stream()
                .findFirst()
                .orElseThrow()
                .getOutputs()
                .getFiles();

        setClasspath(classPath); // EMPTY CLASSPATH, this is MODULE mode
        getMainClass().set("org.mangorage.tsml.bootstrap.Bootstrap");

        setArgs(
                List.of(
                        "--TriviaSpireJar",
                        type == RunType.CLIENT ? Helper.getClient(getProject()).getSingleFile() : Helper.getServer(getProject()).getSingleFile()
                )
        );
    }
}
