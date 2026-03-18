package org.mangorage.tsml.gradle;


import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.mangorage.tsml.gradle.core.TaskRegistry;
import org.mangorage.tsml.gradle.tasks.EncryptTSMLTask;
import org.mangorage.tsml.gradle.tasks.RunTask;

public final class TSMLGradlePlugin implements Plugin<Project> {

    private static DevConfig devConfigStatic;

    public static DevConfig getDevConfig() {
        return devConfigStatic;
    }

    private final DevConfig devConfig = new DevConfig();
    private final TaskRegistry taskRegistry = new TaskRegistry();

    public TaskRegistry getTaskRegistry() {
        return taskRegistry;
    }

    public TSMLGradlePlugin() {
        devConfigStatic = devConfig;

        taskRegistry.register(t -> {
            t.register("runGame", RunTask.class);
            t.register("encryptGame", EncryptTSMLTask.class);
        });
    }

    @Override
    public void apply(Project project) {
        project.getExtensions().add("TSMLDevConfig", devConfig);
        taskRegistry.apply(project);
    }
}