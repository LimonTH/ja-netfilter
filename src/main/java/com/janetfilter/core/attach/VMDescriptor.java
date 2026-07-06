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

package com.janetfilter.core.attach;

/**
 * Descriptor for a Java Virtual Machine instance.
 */
public class VMDescriptor {
    /**
     * Process ID.
     */
    private String id;
    private String className;
    private String args;
    private Boolean old = true;

    /**
     * Create a new VM descriptor.
     *
     * @param id the process ID
     * @param className the main class name
     * @param args the process arguments
     */
    public VMDescriptor(String id, String className, String args) {
        this.id = id;
        this.className = className;
        this.args = args;
    }

    /**
     * Get the process ID.
     *
     * @return the process ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the process ID.
     *
     * @param id the process ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the main class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the main class name.
     *
     * @param className the class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the process arguments.
     *
     * @return the arguments
     */
    public String getArgs() {
        return args;
    }

    /**
     * Set the process arguments.
     *
     * @param args the arguments
     */
    public void setArgs(String args) {
        this.args = args;
    }

    /**
     * Check if this VM was present in the previous list.
     *
     * @return true if VM existed in previous list
     */
    public Boolean getOld() {
        return old;
    }

    /**
     * Set whether this VM was present in the previous list.
     *
     * @param old true if existed in previous list
     */
    public void setOld(Boolean old) {
        this.old = old;
    }

    @Override
    public String toString() {
        return id + " " + className;
    }
}
