package org.nassimus.thread;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public interface BufferedBatchCallable<V>  {
    public abstract void call(V[] batchValues) throws Exception;
}