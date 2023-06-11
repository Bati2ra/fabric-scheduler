package com.bati.fabricscheduler.impl;

import com.bati.fabricscheduler.Mod;

class FabricAsyncDebugger {
    private FabricAsyncDebugger next = null;
    private final int expiry;
    private final Mod mod;
    private final Class<? extends Runnable> clazz;

    FabricAsyncDebugger(final int expiry, final  Mod mod, final Class<? extends Runnable> clazz) {
        this.expiry = expiry;
        this.mod = mod;
        this.clazz = clazz;

    }

    final FabricAsyncDebugger getNextHead(final int time) {
        FabricAsyncDebugger next, current = this;
        while (time > current.expiry && (next = current.next) != null) {
            current = next;
        }
        return current;
    }

    final FabricAsyncDebugger setNext(final FabricAsyncDebugger next) {
        return this.next = next;
    }

    StringBuilder debugTo(final StringBuilder string) {
        for (FabricAsyncDebugger next = this; next != null; next = next.next) {
            string.append(next.mod.getId()).append(':').append(next.clazz.getName()).append('@').append(next.expiry).append(',');
        }
        return string;
    }
}