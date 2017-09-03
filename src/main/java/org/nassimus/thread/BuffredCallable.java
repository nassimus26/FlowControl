package org.nassimus.thread;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class BuffredCallable<V> {

    public abstract void call(Object[] values) throws Throwable;

}