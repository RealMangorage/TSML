package org.mangorage.tsmlmixin.mixin.core;

import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;

import java.nio.file.Path;
import java.util.Map;

public final class MixinContainerImpl extends ContainerHandleVirtual {
    /**
     * Creates a new root container handle.
     *
     * @param name the name
     * @since 1.0.0
     */
    public MixinContainerImpl(final String name) {
        super(name);
    }

    /**
     * Adds a resource to this container.
     *
     * @param name the name
     * @param path the path
     * @since 1.0.0
     */
    public void addResource(final String name, final Path path) {
        this.add(new ResourceContainer(name, path));
    }

    /**
     * Adds a resource to this container.
     *
     * @param entry the entry
     * @since 1.0.0
     */
    public void addResource(final Map.Entry<String, Path> entry) {
        this.add(new ResourceContainer(entry.getKey(), entry.getValue()));
    }

    @Override
    public String toString() {
        return "MixinContainer{name=" + this.getName() + "}";
    }

   static class ResourceContainer extends ContainerHandleURI {
        private final String name;
        private final Path path;

        ResourceContainer(final String name, final Path path) {
            super(path.toUri());

            this.name = name;
            this.path = path;
        }

        public String name() {
            return this.name;
        }

        public Path path() {
            return this.path;
        }

        @Override
        public String toString() {
            return "ResourceContainer{name=" + this.name + ", path=" + this.path + "}";
        }
    }
}
