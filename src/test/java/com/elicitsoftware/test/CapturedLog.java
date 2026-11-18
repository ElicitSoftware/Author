package com.elicitsoftware.test;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Records what one logger and its children write while a test runs, each record flattened to
 * its level, its formatted message and its exception, so a test can assert on what reached the
 * log and on what did not.
 */
public final class CapturedLog extends Handler implements AutoCloseable {

    private final Logger logger;
    private final Level before;
    private final List<String> lines = new CopyOnWriteArrayList<>();

    private CapturedLog(String loggerName) {
        logger = Logger.getLogger(loggerName);
        before = logger.getLevel();
        setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
        logger.addHandler(this);
    }

    /** Starts capturing the named logger; close it to stop. */
    public static CapturedLog of(String loggerName) {
        return new CapturedLog(loggerName);
    }

    @Override
    public void publish(LogRecord record) {
        Object[] params = record.getParameters();
        String message = String.valueOf(record.getMessage());
        if (params != null) {
            for (Object param : params) {
                message += " " + param;
            }
        }
        if (record.getThrown() != null) {
            message += " " + record.getThrown();
        }
        lines.add(record.getLevel() + " " + message);
    }

    /** Every captured record, in order. */
    public List<String> lines() {
        return lines;
    }

    /** The whole capture as one text. */
    public String text() {
        return String.join("\n", lines);
    }

    /** How many records were written at WARNING. */
    public long warnings() {
        return lines.stream().filter(l -> l.startsWith(Level.WARNING.getName() + " ") || l.startsWith("WARN ")).count();
    }

    @Override
    public void flush() {
        // nothing buffered
    }

    @Override
    public void close() {
        logger.removeHandler(this);
        logger.setLevel(before);
    }
}
