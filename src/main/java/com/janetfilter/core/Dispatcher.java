/*
 * Copyright (C) 2026 LimonTH
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

import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.plugin.MyTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * Dispatches class file transformations to registered transformers.
 */
public final class Dispatcher implements ClassFileTransformer {
    private final Environment environment;

    /**
     * Immutable-by-convention snapshot of the registered transformers, published atomically.
     * <p>
     * {@link #transform} reads the snapshot exactly once, so a plugin reload (which replaces
     * the snapshot) can never be observed half-applied.
     * </p>
     */
    private volatile State state = new State();

    /**
     * Create a new dispatcher.
     *
     * @param environment the environment context
     */
    public Dispatcher(Environment environment) {
        this.environment = environment;
    }

    /**
     * Remove every registered transformer.
     * <p>
     * Used before a plugin reload. Already loaded classes have to be retransformed afterwards,
     * because retransformation restarts from the original class file bytes.
     * </p>
     */
    public synchronized void reset() {
        state = new State();
    }

    /**
     * Add a transformer to the dispatcher.
     *
     * @param transformer the transformer to add
     */
    public void addTransformer(MyTransformer transformer) {
        if (null == transformer) {
            return;
        }

        if (environment.isAttachMode() && !transformer.attachMode()) {
            DebugInfo.debug("Transformer: " + transformer.getClass().getName() + " is set to not load in attach mode, ignored.");
            return;
        }

        if (environment.isJavaagentMode() && !transformer.javaagentMode()) {
            DebugInfo.debug("Transformer: " + transformer.getClass().getName() + " is set to not load in -javaagent mode, ignored.");
            return;
        }

        synchronized (this) {
            State next = state.copy();
            String className = transformer.getHookClassName();
            if (null == className) {
                next.globalTransformers.add(transformer);

                if (transformer.isManager()) {
                    next.manageTransformers.add(transformer);
                }
            } else {
                next.classSet.add(className.replace('/', '.'));
                next.transformerMap.computeIfAbsent(className, k -> new ArrayList<>()).add(transformer);
            }

            state = next;
        }
    }

    /**
     * Add multiple transformers from a list.
     *
     * @param transformers list of transformers to add
     */
    public void addTransformers(List<MyTransformer> transformers) {
        if (null == transformers) {
            return;
        }

        for (MyTransformer transformer : transformers) {
            addTransformer(transformer);
        }
    }

    /**
     * Add multiple transformers from an array.
     *
     * @param transformers array of transformers to add
     */
    public void addTransformers(MyTransformer[] transformers) {
        if (null == transformers) {
            return;
        }

        addTransformers(Arrays.asList(transformers));
    }

    /**
     * Get the set of hooked class names.
     *
     * @return unmodifiable set of class names
     */
    public Set<String> getHookClassNames() {
        return Collections.unmodifiableSet(state.classSet);
    }

    /**
     * Transform the class file buffer.
     *
     * @param loader            the class loader
     * @param className         the class name
     * @param classBeingRedefined the class being redefined
     * @param protectionDomain  the protection domain
     * @param classFileBuffer   the class file buffer
     * @return transformed class file buffer
     * @throws IllegalClassFormatException if transformation fails
     */
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        if (null == className) {
            return classFileBuffer;
        }

        // Read the snapshot once so a concurrent reload cannot be observed partially.
        State current = state;
        List<MyTransformer> transformers = current.transformerMap.get(className);
        List<MyTransformer> globalTransformers = null == transformers ? current.manageTransformers : current.globalTransformers;

        int order = 0;

        try {
            for (MyTransformer transformer : globalTransformers) {
                transformer.before(loader, classBeingRedefined, protectionDomain, className, classFileBuffer);
            }

            for (MyTransformer transformer : globalTransformers) {
                classFileBuffer = apply(transformer.preTransform(loader, classBeingRedefined, protectionDomain, className, classFileBuffer, order++), classFileBuffer);
            }

            if (null != transformers) {
                for (MyTransformer transformer : transformers) {
                    classFileBuffer = apply(transformer.transform(loader, classBeingRedefined, protectionDomain, className, classFileBuffer, order++), classFileBuffer);
                }
            }

            for (MyTransformer transformer : globalTransformers) {
                classFileBuffer = apply(transformer.postTransform(loader, classBeingRedefined, protectionDomain, className, classFileBuffer, order++), classFileBuffer);
            }
        } catch (Throwable e) {
            DebugInfo.error("Transform class failed: " + className, e);
        }

        try {
            for (MyTransformer transformer : globalTransformers) {
                transformer.after(loader, classBeingRedefined, protectionDomain, className, classFileBuffer);
            }
        } catch (Throwable e) {
            DebugInfo.error("After transform failed: " + className, e);
        }

        return classFileBuffer;
    }

    /**
     * A {@code null} result means "no transformation" per the {@link ClassFileTransformer}
     * contract, so the previous buffer is kept instead of propagating {@code null} to the
     * next transformer.
     *
     * @param transformed the buffer returned by a transformer (may be null)
     * @param previous    the buffer to fall back to
     * @return the buffer to use from now on
     */
    private static byte[] apply(byte[] transformed, byte[] previous) {
        return null == transformed ? previous : transformed;
    }

    /**
     * Snapshot of the registered transformers.
     * <p>
     * Instances are never mutated after being published through {@link Dispatcher#state}.
     * Mutations are performed on a private copy that is published atomically.
     * </p>
     */
    private static final class State {
        private final Set<String> classSet = new TreeSet<>();
        private final Map<String, List<MyTransformer>> transformerMap = new HashMap<>();
        private final List<MyTransformer> globalTransformers = new ArrayList<>();
        private final List<MyTransformer> manageTransformers = new ArrayList<>();

        private State copy() {
            State copy = new State();
            copy.classSet.addAll(classSet);
            transformerMap.forEach((key, value) -> copy.transformerMap.put(key, new ArrayList<>(value)));
            copy.globalTransformers.addAll(globalTransformers);
            copy.manageTransformers.addAll(manageTransformers);

            return copy;
        }
    }
}
