package org.nassimus.thread;

import java.util.HashMap;
import java.util.Map;

abstract class SimpleObjectPool<T> {
    private final int maxNbObjects;
    private final Map<T, Boolean> objects;
    private T[] objectsArray;
    public SimpleObjectPool(int maxNbObjects){
        this.maxNbObjects = maxNbObjects;
        objects = new HashMap<>(maxNbObjects);
    }

    public abstract T createAnObject();

    public T checkOut() {
        if (objectsArray==null || objectsArray.length<maxNbObjects)
            objectsArray = (T[]) objects.keySet().toArray();
        for (T object : objectsArray) {
            Boolean state = objects.get(object);
            if (state!=null && !state) {
                objects.put(object, true);
                return object;
            }
        }
        T object = createAnObject();
        objects.put(object, true);
        return object;
    }
    public void release(T object) {
        objects.put(object, false);
    }
}
