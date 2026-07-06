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

import com.janetfilter.core.attach.VMSelector;
import com.janetfilter.core.commons.DebugInfo;

import java.io.File;
import java.lang.instrument.Instrumentation;

/**
 * Entry point for the ja-netfilter agent.
 */
public class Launcher {
    /**
     * Main method for launching the agent in attach mode.
     *
     * @param args command line arguments
     */
    static void main(String[] args) {
        if (null != args && args.length > 0) {
            if (args[0].equalsIgnoreCase("--version") || args[0].equalsIgnoreCase("-v")) {
                System.out.println(BuildVersion.getAppName() + " " + BuildVersion.getVersion());
                System.out.println("Dev: " + BuildVersion.getDevName());
                System.out.println("Version number: " + BuildVersion.getVersionNumber());
                return;
            }
        }

        try {
            File agentJar = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().getFile());
            new VMSelector(agentJar).select();
        } catch (Exception e) {
            DebugInfo.error("Launcher main failed", e);
            System.exit(1);
        }
    }

    /**
     * premain method for -javaagent mode.
     *
     * @param agentArgs agent arguments
     * @param inst      instrumentation instance
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        run(agentArgs, inst, false);
    }

    /**
     * agentmain method for attach mode.
     *
     * @param agentArgs agent arguments
     * @param inst      instrumentation instance
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        run(agentArgs, inst, true);
    }

    /**
     * Run the agent initialization.
     *
     * @param agentArgs agent arguments
     * @param inst      instrumentation instance
     * @param attachMode true if in attach mode, false if in javaagent mode
     */
    private static void run(String agentArgs, Instrumentation inst, boolean attachMode) {
        if (null != agentArgs && (agentArgs.equalsIgnoreCase("--version") || agentArgs.equalsIgnoreCase("-v"))) {
            System.out.println(BuildVersion.getAppName() + " " + BuildVersion.getVersion());
            System.out.println("Dev: " + BuildVersion.getDevName());
            System.out.println("Version number: " + BuildVersion.getVersionNumber());
            return;
        }

        DebugInfo.output("========================================");
        DebugInfo.output(BuildVersion.getAppName() + " " + BuildVersion.getVersion());
        DebugInfo.output("Dev: " + BuildVersion.getDevName());
        DebugInfo.output("Mode: " + (attachMode ? "attach" : "premain"));
        DebugInfo.output("========================================");

        Environment environment = new Environment(inst, new java.io.File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().getFile()), attachMode);
        Initializer.init(environment);
    }
}