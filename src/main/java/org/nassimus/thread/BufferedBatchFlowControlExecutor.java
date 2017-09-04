package org.nassimus.thread;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class BufferedBatchFlowControlExecutor<V> extends FlowControlExecutor<V> {

    private BufferedBatchCallable<V> callable;
    private List<V> buffer;
    private int bufferSize;
    /**
     * Name examples :
     * </p>
     * Executor __ SPLITTER
     * </p>
     * Executor __|__ PROCESS
     * </p>
     * Executor __|__|__ WRITER
     *
     * @param callable
     * @param nbThreads
     * @param maxQueueSize
     * @param name
     */
    public BufferedBatchFlowControlExecutor(BufferedBatchCallable<V> callable, int bufferSize, int nbThreads, int maxQueueSize, final String name) {
        super(nbThreads, maxQueueSize, name);
        this.bufferSize = bufferSize;
        this.callable = callable;
        this.buffer = new ArrayList<>();
    }

    public BufferedBatchFlowControlExecutor(int nbThreads, int maxQueueSize, final String name) {
        this(null,0, nbThreads, maxQueueSize, name );
    }

    public void submitWithException(V params) throws Throwable {
        submit(params);
        if (executionExceptions.size() > 0) {
            throw executionExceptions.poll();
        }
    }

    public void submit(V params) throws InterruptedException {
        if (isWorkDone())
            throw new RuntimeException("No more task accepted");
        synchronized (buffer){
            buffer.add(params);
            if (buffer.size()==bufferSize){
                process();
            }
        }
    }
    public abstract boolean isWorkDone();
    private AtomicBoolean processingBatch = new AtomicBoolean();
    private void process() throws InterruptedException{
        synchronized (buffer){
            if (buffer.isEmpty())
                return;
            final V[] vals = (V[]) buffer.toArray();
            buffer.clear();
            processingBatch.set(true);
            submit(new Callable<V>() {
                @Override
                public V call() throws Throwable {
                    callable.call(vals);
                    processingBatch.set(false);
                    return null;
                }
            });
        }
    }

    private boolean shouldFlush(){
        return isWorkDone() && (!buffer.isEmpty() || !isQueueEmpty()) && !processingBatch.get();
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
    private void waitAndFlushAndShutDownWithException(boolean throwException) throws Throwable {
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
                    } else if (isWorkDone() && !processingBatch.get()) {
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