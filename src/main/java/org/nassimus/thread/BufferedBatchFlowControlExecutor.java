package org.nassimus.thread;

import org.nassimus.thread.util.SimpleObjectPool;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class BufferedBatchFlowControlExecutor<T> extends FlowControlExecutor<List<T>> {

    private BufferedBatchCallable<T> callable;
    private List<T> buffer;
    private SimpleObjectPool<List<T>> buffersPool;
    private int bufferSize;

    public BufferedBatchFlowControlExecutor(BufferedBatchCallable<T> callable, final int bufferSize, int nbThreads, int maxQueueSize, final String name) {
        super(nbThreads, maxQueueSize, name);
        this.bufferSize = bufferSize;
        this.callable = callable;
        this.buffer = new ArrayList<>();
        this.buffersPool = new SimpleObjectPool<List<T>>(nbThreads){
            @Override
            public List<T> createAnObject() {
                return new ArrayList<T>(bufferSize);
            }
        };
    }

    public BufferedBatchFlowControlExecutor(int nbThreads, int maxQueueSize, final String name) {
        this(null,0, nbThreads, maxQueueSize, name );
    }
    public void submitWithException(T params) throws Throwable {
        submit(params);
        if (executionExceptions.size() > 0) {
            throw executionExceptions.poll();
        }
    }
    public void submit(T params) throws InterruptedException {
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
        synchronized (this){
            if (buffer.isEmpty())
                return;
            final List<T> newBuffer = buffersPool.checkOut();
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