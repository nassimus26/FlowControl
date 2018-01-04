package org.nassimus.thread;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public interface BufferedBatchCallable<V>  {
    void call(List<V> batchValues);
}