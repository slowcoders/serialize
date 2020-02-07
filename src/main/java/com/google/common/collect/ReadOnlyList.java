package com.google.common.collect;

public abstract class ReadOnlyList<E> extends ImmutableList<E> {

    protected ReadOnlyList() {
    }

    @Override
    boolean isPartialView() {
        return false;
    }
}
