package org.nassimus.thread;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class BufferedBatchFlowControlExecutor<Type, ArrayOfType> extends FlowControlExecutor<ArrayOfType> {

    private BufferedBatchCallable<Type> callable;
    private List<Type> buffer;
    private SimpleObjectPool<List<Type>> buffersPool;
    private int bufferSize;
    private final Class type;

    public BufferedBatchFlowControlExecutor(BufferedBatchCallable<Type> callable, final int bufferSize, int nbThreads, int maxQueueSize, final String name) {
        super(nbThreads, maxQueueSize, name);
        this.bufferSize = bufferSize;
        this.callable = callable;
        this.buffer = new ArrayList<>();
        this.type = getGenericType();
        this.buffersPool = new SimpleObjectPool<List<Type>>(nbThreads){
            @Override
            public List<Type> createAnObject() {
                return new ArrayList<>(bufferSize);
            }
        };
    }

    public BufferedBatchFlowControlExecutor(int nbThreads, int maxQueueSize, final String name) {
        this(null,0, nbThreads, maxQueueSize, name );
    }
    private Class getGenericType(){
        Class clz = getClass();
        java.lang.reflect.Type genericType;
        while((genericType = clz.getGenericSuperclass())!=null &&
                !(ParameterizedType.class.isAssignableFrom(genericType.getClass()))){
            clz = clz.getSuperclass();
        }
        return (Class)((ParameterizedType) clz.getGenericSuperclass()).getActualTypeArguments()[0];
    }
    public void submitWithException(Type params) throws Throwable {
        submit(params);
        if (executionExceptions.size() > 0) {
            throw executionExceptions.poll();
        }
    }
    public void submit(Type params) throws InterruptedException {
        if (isSubmitsEnds())
            throw new RuntimeException("No more task accepted");
        synchronized (buffer){
            buffer.add(params);
            if (buffer.size()==bufferSize){
                process();
            }
        }
    }
    public abstract boolean isSubmitsEnds();

    private void process() throws InterruptedException{
        if (!buffer.isEmpty())
        synchronized (buffer){
            if (buffer.isEmpty())
                return;
            final List<Type> newBuffer = buffersPool.checkOut();
            newBuffer.clear();
            newBuffer.addAll(buffer);
            buffer.clear();
            submit(new Callable() {
                @Override
                public void call() throws Exception {
                    callable.call(newBuffer);
                    buffersPool.release(newBuffer);
                }
            });
        }
    }

    private boolean shouldFlush(){
        return isSubmitsEnds() && !buffer.isEmpty();
    }

    public void waitAndFlushAndShutDown() throws InterruptedException {
        try {
            waitAndFlushAndShutDownWithException(false);
        } catch (Throwable e) {
            if (e instanceof InterruptedException)
                throw (InterruptedException)e;
        }
    }
    public void waitAndFlushAndShutDownWithException() throws Throwable {
        waitAndFlushAndShutDownWithException(true);
    }
    private void waitAndFlushAndShutDownWithException(boolean throwException) throws Exception {
        while(true){
            synchronized (emptyQueueLock) {
                try {
                    if (!isQueueEmpty())
                        emptyQueueLock.wait();
                    if (shouldFlush())
                        process();
                    if (throwException && executionExceptions.size() > 0) {
                        throw executionExceptions.poll();
                    }
                } finally {
                    if (shouldFlush()) {
                        process();
                    } else if ( isSubmitsEnds() && isQueueEmpty() ) {
                        break;
                    }
                }
            }
        }
        executor.shutdown();
        printLogStop();
    }

    @Override
    public String toString(long chunkSize) {
        return super.toString(chunkSize*bufferSize);
    }
}