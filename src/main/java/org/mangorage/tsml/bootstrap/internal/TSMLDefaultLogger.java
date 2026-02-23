package org.mangorage.tsml.bootstrap.internal;

import org.mangorage.tsml.bootstrap.api.logger.ILogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class TSMLDefaultLogger implements ILogger {

    private static final TSMLDefaultLogger INSTANCE = new TSMLDefaultLogger();
    private static final Path LOG_DIR = Paths.get("loader-logs");
    private static final Path LATEST_LOG = LOG_DIR.resolve("latest-loader.txt");
    private static final SimpleDateFormat LINE_TIME = new SimpleDateFormat("HH:mm:ss.SSS");

    private static BufferedWriter writer;

    static {
        try {
            initLogFile();
        } catch (IOException e) {
            throw new RuntimeException("Logger initialization failed", e);
        }
    }

    public static ILogger getInstance() {
        return INSTANCE;
    }

    private static void initLogFile() throws IOException {
        Files.createDirectories(LOG_DIR);

        if (Files.exists(LATEST_LOG)) {
            FileTime lastModified = Files.getLastModifiedTime(LATEST_LOG);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                    .format(new Date(lastModified.toMillis()));

            Path archived = LOG_DIR.resolve("loader-" + timestamp + ".txt");
            Files.move(LATEST_LOG, archived, StandardCopyOption.REPLACE_EXISTING);
        }

        writer = Files.newBufferedWriter(LATEST_LOG,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    TSMLDefaultLogger() {}

    private String getCallingClass() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return stack[4].getClassName();
    }

    private void log(String level, String msg) {
        String time = LINE_TIME.format(new Date());
        String caller = getCallingClass();

        String line = "[" + time + "] [" + level + "] [" + caller + "] " + msg;

        // console
        System.out.println(line);

        // file
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String level, Throwable t) {
        log(level, t.toString());
        for (StackTraceElement el : t.getStackTrace()) {
            log(level, "    at " + el);
        }
    }

    @Override
    public void debug(String log) {
        log("DEBUG", log);
    }

    @Override
    public void info(String log) {
        log("INFO", log);
    }

    @Override
    public void warn(String log) {
        log("WARN", log);
    }

    @Override
    public void error(String log) {
        log("ERROR", log);
    }

    @Override
    public void debug(Throwable throwable) {
        log("DEBUG", throwable);
    }

    @Override
    public void info(Throwable throwable) {
        log("INFO", throwable);
    }

    @Override
    public void warn(Throwable throwable) {
        log("WARN", throwable);
    }

    @Override
    public void error(Throwable throwable) {
        log("ERROR", throwable);
    }
}