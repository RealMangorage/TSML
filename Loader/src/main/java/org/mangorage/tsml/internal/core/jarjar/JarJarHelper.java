package org.mangorage.tsml.internal.core.jarjar;

import com.google.gson.Gson;
import org.mangorage.tsml.internal.core.nested.api.IJar;
import java.util.List;

public final class JarJarHelper {
    private static final Gson GSON = new Gson();

    public static void scanJar(IJar jar, List<JarCouple> jarCouples, boolean builtIn) {
        byte[] metadata = jar.readBytes("META-INF/jarjar/metadata.json");
        if (metadata == null) return;
        JarJarMetadata jarJarMetadata = GSON.fromJson(new String(metadata), JarJarMetadata.class);
        jarJarMetadata.jars()
                .forEach(jarMetadata -> {
                    jarCouples.add(
                            new JarCouple(
                                    jar,
                                    jarMetadata,
                                    builtIn
                            )
                    );
                });
    }
}
