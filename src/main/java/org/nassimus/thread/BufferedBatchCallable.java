package org.nassimus.thread;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class BufferedBatchCallable<V> {

    public abstract void call(Object[] values) throws Throwable;

}