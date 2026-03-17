package org.mangorage.tsml.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.bundling.Jar;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public final class Helper {
    public static void shade(Project project, Jar jar, Configuration configuration) {
        // Use a Callable to lazily resolve the zipTree collection
        jar.from((Callable<Object>) () ->
                        configuration
                                .getFiles()
                                .stream()
                                .map(project::zipTree)
                                .collect(Collectors.toList()),
                copySpec -> {
                    copySpec.into("");
                    copySpec.exclude("META-INF/MANIFEST.MF");
                }
        );
    }

    public static void merge(Project project, Jar jar, Configuration config) {
        // This will only resolve the files when the Jar task executes
        jar.from(config, copySpec -> {
            copySpec.into("JarJarLoader");
        });
    }

}
