/*
 * Original Code by Neo Peng pengzhile@gmail.com
 * Copyright (C) 2026 LimonTH (Modifications and updates)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package com.janetfilter.core;

import com.janetfilter.core.attach.VMLauncher;
import com.janetfilter.core.attach.VMSelector;
import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.utils.WhereIsUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.util.jar.JarFile;

public class Launcher {
    public static final String ATTACH_ARG = "--attach";

    private static boolean loaded = false;

    static void main(String[] args) {
        URI jarURI;
        try {
            jarURI = WhereIsUtils.getJarURI();
        } catch (Throwable e) {
            DebugInfo.error("Can not locate `" + BuildVersion.getAppName() + "` jar file.", e);
            return;
        }

        String jarPath = jarURI.getPath();
        if (args.length > 1 && args[0].equals(ATTACH_ARG)) {
            VMLauncher.attachVM(jarPath, args[1], args.length > 2 ? args[2] : null);
            return;
        }

        printUsage();

        try {
            new VMSelector(new File(jarPath)).select();
        } catch (Throwable e) {
            System.err.println("  ERROR: Select virtual machine failed.");
            e.printStackTrace(System.err);
        }
    }

    public static void premain(String args, Instrumentation inst) {
        premain(args, inst, false);
    }

    public static void agentmain(String args, Instrumentation inst) {
        if (null == System.getProperty("janf.debug")) {
            System.setProperty("janf.debug", "1");
        }

        if (null == System.getProperty("janf.output")) {
            System.setProperty("janf.output", "3");
        }

        premain(args, inst, true);
    }

    private static void premain(String args, Instrumentation inst, boolean attachMode) {
        if (loaded) {
            DebugInfo.warn("You have multiple `" + BuildVersion.getAppName() + "` as javaagent.");
            return;
        }

        printUsage();

        URI jarURI;
        try {
            loaded = true;
            jarURI = WhereIsUtils.getJarURI();
        } catch (Throwable e) {
            DebugInfo.error("Can not locate `" + BuildVersion.getAppName() + "` jar file.", e);
            return;
        }

        File agentFile = new File(jarURI.getPath());
        try {
            inst.appendToBootstrapClassLoaderSearch(new JarFile(agentFile));
        } catch (Throwable e) {
            DebugInfo.error("Can not access `" + BuildVersion.getAppName() + "` jar file.", e);
            return;
        }

        Initializer.init(new Environment(inst, agentFile, args, attachMode)); // for some custom UrlLoaders
    }

    private static void printUsage() {
        String content = "\n  ============================================================================  \n" +
                "\n" +
                "    " + BuildVersion.getAppName() + " " + BuildVersion.getVersion() +
                "\n\n" +
                "    A javaagent framework :)\n" +
                "\n" +
                "    https://github.com/" + BuildVersion.getDevName() + "/" + BuildVersion.getAppName() + "\n" +
                "\n" +
                "  ============================================================================  \n\n";

        System.out.print(content);
    }
}