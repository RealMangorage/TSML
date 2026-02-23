package org.mangorage.tsmlmixin.mixin.services;

import org.mangorage.tsml.api.TSMLLogger;
import org.mangorage.tsml.api.classloader.ITSMLClassloader;
import org.mangorage.tsmlmixin.mixin.SpongeMixinImpl;
import org.mangorage.tsmlmixin.mixin.core.MixinContainerImpl;
import org.mangorage.tsmlmixin.mixin.core.MixinLoggerImpl;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;
import org.spongepowered.asm.transformers.MixinClassReader;
import org.spongepowered.asm.util.IConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;

public final class TSMLMixinServiceImpl extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker  {

    private final MixinContainerImpl container = new MixinContainerImpl("tsmlmixin");
    private IConsumer<MixinEnvironment.Phase> phaseConsumer;

    @Override
    public void prepare() {
        super.prepare();
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void beginPhase() {
        TSMLLogger.getLogger().info("Beginning Mixin Phase: " + MixinEnvironment.getCurrentEnvironment().getPhase());
        if (MixinEnvironment.getCurrentEnvironment().getPhase() == MixinEnvironment.Phase.PREINIT) {
            SpongeMixinImpl.setTransformerFactory(getInternal(IMixinTransformerFactory.class));
        }
        super.beginPhase();
    }

    @Override
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
        super.wire(phase, phaseConsumer);
        if (phase == MixinEnvironment.Phase.PREINIT) {
            phaseConsumer.accept(MixinEnvironment.Phase.DEFAULT);
        }
    }

    @Override
    public synchronized ILogger getLogger(String name) {
        return MixinLoggerImpl.get(name);
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_17;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_17;
    }

    @Override
    public String getName() {
        return "TSMLMixinServiceImpl";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return List.of("org.mangorage.tsmlmixin.mixin.services.MixinPlatformServiceAgentImpl");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return this.container;
    }

    @Override
    public InputStream getResourceAsStream(String s) {
        final var loader = Thread.currentThread().getContextClassLoader();
        return loader.getResourceAsStream(s);
    }

    // Class Provider

    @Override
    public URL[] getClassPath() {
        return ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Class<?> findClass(final String name, final boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Class<?> findAgentClass(final String name, final boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Thread.currentThread().getContextClassLoader());
    }

    // Class Bytecode Provider

    @Override
    public ClassNode getClassNode(final String name) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(final String name, final boolean runTransformers) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, runTransformers, 0);
    }

    public ClassNode getClassNode(final String name, final boolean runTransformers, final int readerFlags) throws ClassNotFoundException, IOException {
        if(!runTransformers) throw new IllegalStateException("ClassNodes must always be provided transformed!");
        ITSMLClassloader loader = (ITSMLClassloader) Thread.currentThread().getContextClassLoader();

        final String canonicalName = name.replace('/', '.');
        final String internalName = name.replace('.', '/');

        final var classBytes = loader.getClassBytes(canonicalName);

        return classNode(canonicalName, internalName, classBytes, readerFlags);
    }

    public ClassNode classNode(final String canonicalName, final String internalName, final byte[] input, final int readerFlags) throws ClassNotFoundException {
        if (input.length != 0) {
            final ClassNode node = new ClassNode(Opcodes.ASM9);
            final ClassReader reader = new MixinClassReader(input, canonicalName);
            reader.accept(node, readerFlags);
            return node;
        }
        throw new ClassNotFoundException(canonicalName);
    }

    // Transformer Provider

    @Override
    public Collection<ITransformer> getTransformers() {
        return List.of();
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers() {
        return List.of();
    }

    @Override
    public void addTransformerExclusion(String name) {

    }

    // Class Tracker

    @Override
    public void registerInvalidClass(String className) {

    }

    @Override
    public boolean isClassLoaded(String name) {
        final ITSMLClassloader loader = (ITSMLClassloader) Thread.currentThread().getContextClassLoader();
        return loader.hasClass(name);
    }

    @Override
    public String getClassRestrictions(String className) {
        return "";
    }
}
