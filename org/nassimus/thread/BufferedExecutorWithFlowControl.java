package org.nassimus.thread;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BufferedExecutorWithFlowControl<V> extends ExecutorWithFlowControl<V>{

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
    public BufferedExecutorWithFlowControl(BuffredCallable<V> callable, int bufferSize, int nbThreads, int maxQueueSize, final String name) {
        super(nbThreads, maxQueueSize, name);
        this.bufferSize = bufferSize;
        this.callable = callable;
        this.buffer = new ArrayList<>();
    }

    public BufferedExecutorWithFlowControl(int nbThreads, int maxQueueSize, final String name) {
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
                }else if (isWorkDone() && !working.get())
                    break;
            }
        }
        executor.shutdown();
        printLogStop();
    }
}