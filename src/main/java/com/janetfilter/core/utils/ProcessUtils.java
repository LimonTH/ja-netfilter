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

package com.janetfilter.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Process utilities for managing and redirecting process output.
 */
public class ProcessUtils {
    /**
     * Cached process ID.
     */
    private static String processId;

    /**
     * Get the current process ID.
     *
     * @return the process ID
     */
    public synchronized static String currentId() {
        if (null == processId) {
            String name = ManagementFactory.getRuntimeMXBean().getName() + "@";

            processId = name.split("@", 2)[0];
        }

        return processId;
    }

    /**
     * Start a process with default output streams.
     *
     * @param pb the process builder
     * @return the exit code
     * @throws Exception if process fails
     */
    public static int start(ProcessBuilder pb) throws Exception {
        return start(pb, System.out, System.err);
    }

    /**
     * Start a process with custom output stream.
     *
     * @param pb the process builder
     * @param out the output stream
     * @return the exit code
     * @throws Exception if process fails
     */
    public static int start(ProcessBuilder pb, OutputStream out) throws Exception {
        return start(pb, out, null);
    }

    /**
     * Start a process with custom output and error streams.
     *
     * @param pb the process builder
     * @param out the output stream
     * @param err the error stream
     * @return the exit code
     * @throws Exception if process fails
     */
    public static int start(ProcessBuilder pb, OutputStream out, OutputStream err) throws Exception {
        Process p = pb.start();

        List<Thread> threads = new ArrayList<>();
        if (null != out) {
            threads.add(new Thread(new RedirectOutput(p.getInputStream(), out)));
        }
        if (null != err) {
            threads.add(new Thread(new RedirectOutput(p.getErrorStream(), err)));
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        return p.waitFor();
    }

    static class RedirectOutput implements Runnable {
        private static final int BUFF_SIZE = 1024;
        private final InputStream origin;
        private final OutputStream dest;

        RedirectOutput(InputStream origin, OutputStream dest) {
            this.origin = origin;
            this.dest = dest;
        }

        public void run() {
            int length;
            byte[] buffer = new byte[BUFF_SIZE];

            try {
                while ((length = origin.read(buffer)) != -1) {
                    dest.write(buffer, 0, length);
                }
            } catch (IOException e) {
                throw new RuntimeException("ERROR: Redirect output failed.", e);
            }
        }
    }
}
