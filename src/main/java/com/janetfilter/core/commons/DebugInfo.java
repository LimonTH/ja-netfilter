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

import com.janetfilter.core.utils.DateUtils;
import com.janetfilter.core.utils.ProcessUtils;
import com.janetfilter.core.utils.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Debug and logging utility for console and file output.
 * Standalone implementation without external logging dependencies.
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

    private static final ExecutorService CONSOLE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final ExecutorService FILE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String CLASS_NAME = DebugInfo.class.getName();
    private static final String LOG_TEMPLATE = "%s %-5s [%s@%-5s] %s-%d : %s%n";
    private static final String PID = ProcessUtils.currentId();
    private static final Level LOG_LEVEL;
    private static final Long LOG_OUTPUT;
    private static File logDir;

    static {
        Level level = Level.of(System.getProperty("janf.debug"));
        LOG_LEVEL = Level.NONE == level ? Level.of(System.getenv("JANF_DEBUG")) : level;

        Long output = StringUtils.toLong(System.getProperty("janf.output"));
        if (null == output) {
            output = StringUtils.toLong(System.getenv("JANF_OUTPUT"));
        }
        LOG_OUTPUT = null == output ? OUTPUT_CONSOLE : output;
    }

    /**
     * Set the log directory for file output.
     *
     * @param dir the log directory
     */
    public static void useFile(File dir) {
        if (Level.NONE == LOG_LEVEL || 0L == (LOG_OUTPUT & OUTPUT_FILE) || null == dir) {
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

        logDir = dir;
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

        if (0L != (LOG_OUTPUT & OUTPUT_CONSOLE)) {
            CONSOLE_EXECUTOR.execute(new ConsoleWriteTask(PID, level, content, e));
        }

        if (null != logDir) {
            FILE_EXECUTOR.execute(new FileWriteTask(logDir, PID, level, content, e));
        }
    }

    public enum Level {
        NONE, DEBUG, INFO, WARN, ERROR;

        public static Level of(String valueStr) {
            int value;
            if (null == valueStr) {
                return NONE;
            }

            try {
                value = Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
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

    private static class FileWriteTask extends ConsoleWriteTask {
        private final File logDir;

        FileWriteTask(File logDir, String pid, Level level, String content, Throwable exception) {
            super(pid, level, content, exception);
            this.logDir = logDir;
        }

        @Override
        public void run() {
            String fileName = String.format("%s%s.log",
                    DateUtils.formatDate(this.getDateTime()),
                    0L != (LOG_OUTPUT & OUTPUT_WITH_PID) ? "-" + this.getPID() : "");

            try (PrintStream ps = new PrintStream(new FileOutputStream(new File(this.logDir, fileName), true))) {
                this.setPrintStream(ps);
                super.run();
            } catch (FileNotFoundException e) {
                writeException("log file not found!", e, System.err);
            }
        }
    }

    private static class ConsoleWriteTask implements Runnable {
        private final String pid;
        private final Level level;
        private final String content;
        private final Throwable exception;
        private final Throwable stackException;
        private final String threadName;
        private final Date dateTime;
        private PrintStream ps;

        ConsoleWriteTask(String pid, Level level, String content, Throwable exception) {
            this.pid = pid;
            this.level = level;
            this.content = content;
            this.exception = exception;
            this.stackException = new Throwable();
            this.threadName = Thread.currentThread().getName();
            this.dateTime = new Date();
            this.setPrintStream(null == exception ? System.out : System.err);
        }

        protected static void writeContent(String content, PrintStream ps) {
            if (null == ps) {
                return;
            }
            ps.print(content);
        }

        protected static void writeException(String content, Throwable e, PrintStream ps) {
            if (null == ps) {
                return;
            }
            ps.print(content);
            e.printStackTrace(ps);
        }

        protected static void write(String content, Throwable e, PrintStream stream) {
            if (null == e) {
                writeContent(content, stream);
                return;
            }
            writeException(content, e, stream);
        }

        protected PrintStream getPrintStream() {
            return this.ps;
        }

        protected void setPrintStream(PrintStream ps) {
            this.ps = ps;
        }

        protected String getPID() {
            return this.pid;
        }

        public Date getDateTime() {
            return this.dateTime;
        }

        @Override
        public void run() {
            int line = 0;
            String caller = "UNKNOWN";
            StackTraceElement[] traces = this.stackException.getStackTrace();
            int l = traces.length;

            for (int i = 1; i < l; ++i) {
                StackTraceElement element = traces[i];
                if (CLASS_NAME.equals(element.getClassName())) {
                    continue;
                }
                line = element.getLineNumber();
                caller = element.getClassName();
                break;
            }

            String outContent = String.format(LOG_TEMPLATE,
                    DateUtils.formatDateTimeMicro(this.dateTime),
                    this.level,
                    this.threadName,
                    this.pid,
                    caller,
                    line,
                    this.content);

            write(outContent, this.exception, this.getPrintStream());
        }
    }
}
