package org.mangorage.tsmlmixin.mixin.core;

import org.mangorage.tsml.api.TSMLLogger;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MixinLoggerImpl extends LoggerAdapterAbstract {

    private static final Map<String, ILogger> LOGGER_MAP = new ConcurrentHashMap<>();

    public static ILogger get(String name) {
        return LOGGER_MAP.computeIfAbsent(name, MixinLoggerImpl::new);
    }

    private MixinLoggerImpl(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "TSMLMixinLogger";
    }

    @Override
    public void catching(Level level, Throwable throwable) {
        forwardThrowable(level, throwable);
    }

    @Override
    public void log(Level level, String s, Object... objects) {
        String msg = formatBraces(s, objects);
        forwardMessage(level, "[" + getId() + "] " + msg);
    }

    @Override
    public void log(Level level, String s, Throwable throwable) {
        String msg = formatBraces(s);
        forwardMessage(level,"[" + getId() + "] " + msg);
        forwardThrowable(level, throwable);
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        TSMLLogger.getLogger().error(t);
        return t;
    }

    /* ================== FORWARDING ================== */

    private void forwardMessage(Level level, String msg) {
        switch (level) {
            case DEBUG -> TSMLLogger.getLogger().debug(msg);
            case WARN  -> TSMLLogger.getLogger().warn(msg);
            case ERROR, FATAL -> TSMLLogger.getLogger().error(msg);
            default -> TSMLLogger.getLogger().info(msg);
        }
    }

    private void forwardThrowable(Level level, Throwable t) {
        switch (level) {
            case DEBUG -> TSMLLogger.getLogger().debug(t);
            case INFO  -> TSMLLogger.getLogger().info(t);
            case WARN  -> TSMLLogger.getLogger().warn(t);
            default -> TSMLLogger.getLogger().error(t);
        }
    }

    /* ================== {} FORMATTER ================== */

    private static String formatBraces(String msg, Object... args) {
        if (msg == null || args == null || args.length == 0) return msg;

        StringBuilder sb = new StringBuilder(msg);
        for (Object arg : args) {
            int idx = sb.indexOf("{}");
            if (idx == -1) break;
            sb.replace(idx, idx + 2, String.valueOf(arg));
        }
        return sb.toString();
    }
}