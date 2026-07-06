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

package com.janetfilter.core.utils;

import com.janetfilter.core.commons.Logger;

/**
 * Runtime environment detection.
 */
public final class EnvironmentDetector {
    private static final org.slf4j.Logger LOG = Logger.getLogger(EnvironmentDetector.class);

    private EnvironmentDetector() {
    }

    public static EnvironmentType detect() {
        if (isContainer()) {
            return EnvironmentType.CONTAINER;
        }
        if (isIDEA()) {
            return EnvironmentType.IDEA;
        }
        if (isCI()) {
            return EnvironmentType.CI;
        }
        return EnvironmentType.UNKNOWN;
    }

    private static boolean isContainer() {
        String containerEnv = System.getenv("CONTAINER");
        if (null != containerEnv && !containerEnv.isEmpty()) {
            LOG.debug("Container detected via CONTAINER env");
            return true;
        }

        String dockerEnv = System.getenv("DOCKER_CONTAINER");
        if (null != dockerEnv && !dockerEnv.isEmpty()) {
            LOG.debug("Container detected via DOCKER_CONTAINER env");
            return true;
        }

        if (System.getProperty("java.vm.name", "").toLowerCase().contains("docker")) {
            LOG.debug("Container detected via JVM property");
            return true;
        }

        return false;
    }

    private static boolean isIDEA() {
        String ideaHome = System.getProperty("idea.home");
        if (null != ideaHome && !ideaHome.isEmpty()) {
            LOG.debug("IDEA detected via idea.home");
            return true;
        }

        String ideaClassPath = System.getProperty("idea.class.path");
        if (null != ideaClassPath && !ideaClassPath.isEmpty()) {
            LOG.debug("IDEA detected via idea.class.path");
            return true;
        }

        return false;
    }

    private static boolean isCI() {
        String ciEnv = System.getenv("CI");
        if (null != ciEnv && !ciEnv.isEmpty() && !"false".equalsIgnoreCase(ciEnv)) {
            LOG.debug("CI detected via CI env");
            return true;
        }

        String continuousIntegration = System.getenv("CONTINUOUS_INTEGRATION");
        if (null != continuousIntegration && !continuousIntegration.isEmpty() && !"false".equalsIgnoreCase(continuousIntegration)) {
            LOG.debug("CI detected via CONTINUOUS_INTEGRATION env");
            return true;
        }

        return false;
    }

    public enum EnvironmentType {
        UNKNOWN,
        CONTAINER,
        IDEA,
        CI
    }
}