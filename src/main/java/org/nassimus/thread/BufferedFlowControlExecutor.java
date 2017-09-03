package org.nassimus.thread;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class BufferedFlowControlExecutor<V> extends FlowControlExecutor<V> {

    private BuffredCallable<V> callable;
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
    public BufferedFlowControlExecutor(BuffredCallable<V> callable, int bufferSize, int nbThreads, int maxQueueSize, final String name) {
        super(nbThreads, maxQueueSize, name);
        this.bufferSize = bufferSize;
        this.callable = callable;
        this.buffer = new ArrayList<>();
    }

    public BufferedFlowControlExecutor(int nbThreads, int maxQueueSize, final String name) {
        this(null,0, nbThreads, maxQueueSize, name );
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
    private AtomicBoolean working = new AtomicBoolean();
    private void process() throws InterruptedException{
        synchronized (buffer){
            if (buffer.isEmpty())
                return;
            final V[] vals = (V[]) buffer.toArray();
            buffer.clear();
            working.set(true);
            submit(new Callable<V>() {
                @Override
                public V call() throws Throwable {
                    callable.call(vals);
                    working.set(false);
                    return null;
                }
            });
        }
    }

    private boolean shouldFlush(){
        if (buffer==null)
            return false;
        return isWorkDone() && (!buffer.isEmpty() || semaphore.availablePermits() != nbTotalTasks) && !working.get();
    }

    public void waitAndFlushAndShutDown() throws InterruptedException {
        while(true){
            try {
                if (shouldFlush())
                    process();
                Thread.sleep(100);
            }finally {
                if (shouldFlush()) {
                    process();
                }else if (isWorkDone() && !working.get()) {
                    break;
                }
            }
        }
        executor.shutdown();
        printLogStop();
    }
    public void waitAndFlushAndShutDownWithException() throws Throwable {
        while(true){
            try {
                if (shouldFlush())
                    process();
                if (executionExceptions.size() > 0) {
                    throw executionExceptions.poll();
                }
                Thread.sleep(100);
            }finally {
                if (shouldFlush()) {
                    process();
                }else if (isWorkDone() && !working.get()) {
                    break;
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