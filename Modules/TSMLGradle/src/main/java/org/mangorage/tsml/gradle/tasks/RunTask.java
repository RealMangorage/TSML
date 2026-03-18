package org.mangorage.tsml.gradle.tasks;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.JavaExec;
import org.mangorage.tsml.gradle.Helper;
import org.mangorage.tsml.gradle.TSMLGradlePlugin;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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

        List<File> files = new ArrayList<>();

        final var manualLoader = TSMLGradlePlugin.getDevConfig().getLoaderFile();
        if (manualLoader != null && manualLoader.exists()) {
            files.add(manualLoader);
        }

        setClasspath(getProject().files(files));
        getMainClass().set("org.mangorage.tsml.bootstrap.Bootstrap");

        setArgs(
                List.of(
                        "--TriviaSpireJar",
                        type == RunType.CLIENT ? Helper.getClient(getProject()).getSingleFile() : Helper.getServer(getProject()).getSingleFile()
                )
        );
    }
}
