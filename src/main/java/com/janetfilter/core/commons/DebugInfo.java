/*
 *
 *  * Original Code by Neo Peng pengzhile@gmail.com
 *  * Copyright (C) 2026 LimonTH (Modifications and updates)
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://gnu.org>.
 *
 */

package com.janetfilter.core.commons;

import com.janetfilter.core.utils.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Debug and logging utility for console and file output.
 * Delegates to SLF4J for actual logging.
 */
public class DebugInfo {
    /**
     * Output to console flag.
     */
    public static final long OUTPUT_CONSOLE = 0x1L;
    /**
     * Output to file flag.
     */
    public static final long OUTPUT_FILE = 0x2L;
    /**
     * Include PID in output flag.
     */
    public static final long OUTPUT_WITH_PID = 0x4L;

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DebugInfo.class);
    private static final Level LOG_LEVEL;
    private static final Long LOG_OUTPUT;

    static {
        Level level = Level.of(System.getProperty("janf.debug"));
        LOG_LEVEL = Level.NONE == level ? Level.of(System.getenv("JANF_DEBUG")) : level;

        Long output = StringUtils.toLong(System.getProperty("janf.output"));
        if (null == output) {
            output = StringUtils.toLong(System.getenv("JANF_OUTPUT"));
        }
        LOG_OUTPUT = null == output ? OUTPUT_CONSOLE : output;

        if (Level.NONE != LOG_LEVEL && 0 != (LOG_OUTPUT & OUTPUT_FILE)) {
            String logDir = System.getProperty("janetfilter.logs.dir");
            if (null != logDir) {
                System.setProperty("janetfilter.logs.dir", logDir);
            }
        }
    }

    /**
     * Set the log directory for file output.
     *
     * @param dir the log directory
     */
    public static void useFile(File dir) {
        if (Level.NONE == LOG_LEVEL || 0 == (LOG_OUTPUT & OUTPUT_FILE) || null == dir) {
            return;
        }

        if (!dir.exists() && !dir.mkdirs()) {
            error("Can't make directory: " + dir);
            return;
        }

        if (!dir.isDirectory()) {
            error("It's not a directory: " + dir);
            return;
        }

        if (!dir.canWrite()) {
            error("Read-only directory: " + dir);
            return;
        }

        System.setProperty("janetfilter.logs.dir", dir.getAbsolutePath());
    }

    /**
     * Get the current log level.
     *
     * @return the log level
     */
    public static Level getLogLevel() {
        return LOG_LEVEL;
    }

    /**
     * Get the log output configuration.
     *
     * @return the log output flags
     */
    public static long getLogOutput() {
        return LOG_OUTPUT;
    }

    /**
     * Output debug message.
     *
     * @param content the message content
     * @param e       the throwable (may be null)
     */
    public static void debug(String content, Throwable e) {
        output(Level.DEBUG, content, e);
    }

    /**
     * Output debug message.
     *
     * @param content the message content
     */
    public static void debug(String content) {
        debug(content, null);
    }

    /**
     * Output info message.
     *
     * @param content the message content
     * @param e       the throwable (may be null)
     */
    public static void info(String content, Throwable e) {
        output(Level.INFO, content, e);
    }

    /**
     * Output info message.
     *
     * @param content the message content
     */
    public static void info(String content) {
        info(content, null);
    }

    /**
     * Output warning message.
     *
     * @param content the message content
     * @param e       the throwable (may be null)
     */
    public static void warn(String content, Throwable e) {
        output(Level.WARN, content, e);
    }

    /**
     * Output warning message.
     *
     * @param content the message content
     */
    public static void warn(String content) {
        warn(content, null);
    }

    /**
     * Output error message.
     *
     * @param content the message content
     * @param e       the throwable (may be null)
     */
    public static void error(String content, Throwable e) {
        output(Level.ERROR, content, e);
    }

    /**
     * Output error message.
     *
     * @param content the message content
     */
    public static void error(String content) {
        error(content, null);
    }

    /**
     * Output message at default level.
     *
     * @param content the message content
     */
    public static void output(String content) {
        debug(content);
    }

    /**
     * Output message at default level.
     *
     * @param content the message content
     * @param e       the throwable (may be null)
     */
    public static void output(String content, Throwable e) {
        debug(content, e);
    }

    /**
     * Output message at specified level.
     *
     * @param level   the log level
     * @param content the message content
     * @param e       the throwable (may be null)
     */
    public static void output(Level level, String content, Throwable e) {
        if (Level.NONE == LOG_LEVEL || level.ordinal() < LOG_LEVEL.ordinal()) {
            return;
        }

        if (0 == LOG_OUTPUT) {
            return;
        }

        switch (level) {
            case DEBUG -> LOG.debug(content, e);
            case INFO -> LOG.info(content, e);
            case WARN -> LOG.warn(content, e);
            case ERROR -> LOG.error(content, e);
            default -> {
            }
        }
    }

    public enum Level {
        NONE, DEBUG, INFO, WARN, ERROR;

        public static Level of(String valueStr) {
            if (null == valueStr || valueStr.isEmpty()) {
                return NONE;
            }

            int value;
            try {
                value = Integer.parseInt(valueStr);
            } catch (NumberFormatException ex) {
                return NONE;
            }

            for (Level level : values()) {
                if (level.ordinal() == value) {
                    return level;
                }
            }

            return NONE;
        }
    }
}