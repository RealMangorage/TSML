package org.mangorage.tsml.gradle;


import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.mangorage.tsml.gradle.core.TaskRegistry;
import org.mangorage.tsml.gradle.tasks.RunTask;

public final class TSMLGradlePlugin implements Plugin<Project> {


    public static Config cfg = null;

    private final Config config = new Config();
    private final TaskRegistry taskRegistry = new TaskRegistry();

    public TaskRegistry getTaskRegistry() {
        return taskRegistry;
    }

    public Config getConfig() {
        return config;
    }

    public TSMLGradlePlugin() {
        taskRegistry.register(t -> {
            TSMLGradlePlugin.cfg = config;

            t.register("runGame", RunTask.class);
        });
    }

    @Override
    public void apply(Project project) {
        project.getExtensions().add("TSMLConfig", config);

        project.getConfigurations().create("mod", t -> {
            t.setVisible(true);
        });

        taskRegistry.apply(project);
    }
}
