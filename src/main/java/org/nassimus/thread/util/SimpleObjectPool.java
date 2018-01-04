package org.nassimus.thread.util;

import java.util.HashMap;
import java.util.Map;

public abstract class SimpleObjectPool<T> {

    private final Map<T, Boolean> objects;
    private T[] objectsArray;

    public SimpleObjectPool(int maxNbObjects){
        objects = new HashMap<>(maxNbObjects);
    }

    public abstract T createAnObject();

    public T checkOut() {
        if (objectsArray!=null)
            for (T object : objectsArray) {
                Boolean state = objects.get(object);
                if (state!=null && !state) {
                    objects.put(object, true);
                    return object;
                }
            }
        T object = createAnObject();
        objects.put(object, true);
        synchronized (this) {
            objectsArray = (T[]) objects.keySet().toArray();
        }
        return object;
    }

    public void release(T object) {
        synchronized (this){
            objects.put(object, false);
        };
    }

}
