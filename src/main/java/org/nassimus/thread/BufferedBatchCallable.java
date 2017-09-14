package org.nassimus.thread;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public interface BufferedBatchCallable<V>  {
    public abstract void call(List<V> batchValues) throws Exception;
}