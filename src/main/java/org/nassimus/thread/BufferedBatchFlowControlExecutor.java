package org.nassimus.thread;

import org.nassimus.thread.util.SimpleObjectPool;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ThreadFactory;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class BufferedBatchFlowControlExecutor<T> extends FlowControlExecutor<List<T>> {

    private BufferedBatchCallable<T> callable;
    private List<T> buffer;
    private SimpleObjectPool<List<T>> buffersPool;
    private int bufferSize;
    public BufferedBatchFlowControlExecutor(BufferedBatchCallable<T> callable, final int bufferSize, int nbThreads, int maxQueueSize, final ThreadFactory threadFactory) {
        super(nbThreads, maxQueueSize, threadFactory);
        init(callable, bufferSize, nbThreads, maxQueueSize);
    }
    public BufferedBatchFlowControlExecutor(BufferedBatchCallable<T> callable, final int bufferSize, int nbThreads, int maxQueueSize, final String name) {
        super(nbThreads, maxQueueSize, name);
        init(callable, bufferSize, nbThreads, maxQueueSize);
    }
    private void init(BufferedBatchCallable<T> callable, final int bufferSize, int nbThreads, int maxQueueSize){
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
            submit(new Runnable() {
                @Override
                public void run() {
                    callable.call(newBuffer);
                    buffersPool.release(newBuffer);
                }
            });
        }
    }




    private boolean shouldFlush(){
        return isSubmitsEnds() && !buffer.isEmpty();
    }

    public void waitAndFlush(boolean shutdown) throws InterruptedException {
        try {
            waitAndFlush(false, shutdown);
        } catch (Throwable e) {
            if (e instanceof InterruptedException)
                throw (InterruptedException)e;
        }
    }
    public void waitAndFlushWithException(boolean shutdown) throws Throwable {
        waitAndFlush(true, shutdown);
    }

    @Override
    protected void wait(boolean throwException, boolean shutdown) throws Exception {
        waitAndFlush(throwException, shutdown);
    }
    protected void waitAndFlush(boolean throwException, boolean shutdown) throws Exception {
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
        if (shutdown) {
            executor.shutdown();
            printLogStop();
        }
    }

    @Override
    public String toString(long chunkSize) {
        return super.toString(chunkSize*bufferSize);
    }
}