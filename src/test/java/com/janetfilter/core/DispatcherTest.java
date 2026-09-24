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

import static org.junit.jupiter.api.Assertions.*;

import com.janetfilter.core.plugin.MyTransformer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/**
 * Tests for Dispatcher.
 */
public class DispatcherTest {
    private static final String HOOK_CLASS = "com/example/Target";
    private static final String HOOK_CLASS_DOTTED = "com.example.Target";

    private static Dispatcher dispatcher(boolean attachMode) throws IOException {
        File base = Files.createTempDirectory("janf-dispatcher").toFile();

        return new Dispatcher(new Environment(null, new File(base, "ja-netfilter.jar"), attachMode));
    }

    @Test
    public void testGetHookClassNamesShouldReturnDottedNames() throws Exception {
        Dispatcher dispatcher = dispatcher(false);
        dispatcher.addTransformer(new FixedResultTransformer(HOOK_CLASS, new byte[]{1}));

        assertTrue(dispatcher.getHookClassNames().contains(HOOK_CLASS_DOTTED));
    }

    @Test
    public void testGetHookClassNamesShouldBeUnmodifiable() throws Exception {
        Dispatcher dispatcher = dispatcher(false);

        assertThrows(UnsupportedOperationException.class, () -> dispatcher.getHookClassNames().add("com.example.Other"));
    }

    @Test
    public void testResetShouldRemoveEveryHook() throws Exception {
        Dispatcher dispatcher = dispatcher(false);
        dispatcher.addTransformer(new FixedResultTransformer(HOOK_CLASS, new byte[]{1}));
        assertFalse(dispatcher.getHookClassNames().isEmpty());

        dispatcher.reset();

        assertTrue(dispatcher.getHookClassNames().isEmpty());
        assertArrayEquals(new byte[]{7}, dispatcher.transform(null, HOOK_CLASS, null, null, new byte[]{7}));
    }

    @Test
    public void testTransformShouldKeepPreviousBufferWhenTransformerReturnsNull() throws Exception {
        // A null result means "no transformation"; it must never be handed to the next
        // transformer as the class file buffer.
        Dispatcher dispatcher = dispatcher(false);
        dispatcher.addTransformer(new FixedResultTransformer(HOOK_CLASS, null));
        CapturingTransformer second = new CapturingTransformer(HOOK_CLASS, new byte[]{42});
        dispatcher.addTransformer(second);

        byte[] original = new byte[]{1, 2, 3};
        byte[] result = dispatcher.transform(null, HOOK_CLASS, null, null, original);

        assertArrayEquals(original, second.captured);
        assertArrayEquals(new byte[]{42}, result);
    }

    @Test
    public void testTransformShouldPassOriginalBufferToFirstTransformer() throws Exception {
        Dispatcher dispatcher = dispatcher(false);
        CapturingTransformer first = new CapturingTransformer(HOOK_CLASS, new byte[]{5});
        dispatcher.addTransformer(first);

        byte[] original = new byte[]{9, 8};
        dispatcher.transform(null, HOOK_CLASS, null, null, original);

        assertArrayEquals(original, first.captured);
    }

    @Test
    public void testTransformShouldNotPropagateTransformerFailure() throws Exception {
        Dispatcher dispatcher = dispatcher(false);
        dispatcher.addTransformer(new FailingGlobalTransformer());
        dispatcher.addTransformer(new FixedResultTransformer(HOOK_CLASS, new byte[]{1}));

        byte[] original = new byte[]{3, 3};

        byte[] result = assertDoesNotThrow(() -> dispatcher.transform(null, HOOK_CLASS, null, null, original));

        assertArrayEquals(original, result);
    }

    @Test
    public void testTransformShouldCallAfterWhenTransformationFailed() throws Exception {
        Dispatcher dispatcher = dispatcher(false);
        FailingGlobalTransformer failing = new FailingGlobalTransformer();
        dispatcher.addTransformer(failing);
        dispatcher.addTransformer(new FixedResultTransformer(HOOK_CLASS, new byte[]{1}));

        dispatcher.transform(null, HOOK_CLASS, null, null, new byte[]{1});

        assertTrue(failing.afterCalled, "after() must run even when the transformation failed");
    }

    @Test
    public void testTransformShouldReturnOriginalBufferWhenClassIsNotHooked() throws Exception {
        Dispatcher dispatcher = dispatcher(false);
        dispatcher.addTransformer(new FixedResultTransformer(HOOK_CLASS, new byte[]{1}));

        byte[] original = new byte[]{4, 5, 6};
        byte[] result = dispatcher.transform(null, "com/example/Other", null, null, original);

        assertArrayEquals(original, result);
    }

    @Test
    public void testTransformShouldReturnBufferWhenClassNameIsNull() throws Exception {
        Dispatcher dispatcher = dispatcher(false);
        byte[] original = new byte[]{1};

        assertArrayEquals(original, dispatcher.transform(null, null, null, null, original));
    }

    @Test
    public void testAddTransformerShouldIgnoreJavaagentOnlyTransformerInAttachMode() throws Exception {
        Dispatcher dispatcher = dispatcher(true);
        dispatcher.addTransformer(new ModeRestrictedTransformer());

        assertTrue(dispatcher.getHookClassNames().isEmpty());
    }

    @Test
    public void testAddTransformerShouldAcceptJavaagentOnlyTransformerInJavaagentMode() throws Exception {
        Dispatcher dispatcher = dispatcher(false);
        dispatcher.addTransformer(new ModeRestrictedTransformer());

        assertFalse(dispatcher.getHookClassNames().isEmpty());
    }

    /**
     * Transformer that returns a fixed result and records the buffer it received.
     */
    private static class CapturingTransformer implements MyTransformer {
        private final String hookClassName;
        private final byte[] result;
        private byte[] captured;

        CapturingTransformer(String hookClassName, byte[] result) {
            this.hookClassName = hookClassName;
            this.result = result;
        }

        @Override
        public String getHookClassName() {
            return hookClassName;
        }

        @Override
        public byte[] transform(String className, byte[] classBytes, int order) {
            captured = classBytes;

            return result;
        }
    }

    /**
     * Transformer that always returns the same bytes.
     */
    private record FixedResultTransformer(String hookClassName,byte[] result) implements MyTransformer {

        @Override
        public String getHookClassName() {
            return hookClassName;
        }

        @Override
        public byte[] transform(String className, byte[] classBytes, int order) {
            return result;
        }
    }

    /**
     * Global transformer that always fails and records whether after() was called.
     */
    private static class FailingGlobalTransformer implements MyTransformer {
        private boolean afterCalled = false;

        @Override
        public String getHookClassName() {
            return null;
        }

        @Override
        public byte[] transform(String className, byte[] classBytes, int order) throws Exception {
            throw new IllegalStateException("boom");
        }

        @Override
        public void after(String className, byte[] classBytes) {
            afterCalled = true;
        }
    }

    /**
     * Transformer that only loads in -javaagent mode.
     */
    private static class ModeRestrictedTransformer implements MyTransformer {
        @Override
        public String getHookClassName() {
            return HOOK_CLASS;
        }

        @Override
        public boolean attachMode() {
            return false;
        }
    }
}
