package org.nassimus.thread;

public abstract class BuffredCallable<V> {

    public abstract void call(Object[] values) throws Throwable;

}