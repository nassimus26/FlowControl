package org.nassimus.thread;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class FlowControlExecutor<V> {

    private static Runtime runtime = Runtime.getRuntime();
    private static int mb = 1024 * 1024;

    private DecimalFormat decimalFormat = new DecimalFormat();

    protected Queue<Throwable> executionExceptions = new LinkedBlockingQueue<Throwable>();

    protected Semaphore semaphore = null;
    private String name = null;
    protected ThreadPoolExecutor executor = null;
    protected int nbTotalTasks = 0;

    private long timeMilliStart;
    private long timeMilliLast;
    private long nbTaskExecutedLast;

    private final AtomicInteger counterForName = new AtomicInteger();

    private Timer timer = null;
    private BufferedBatchCallable<V> callable;
    protected Object emptyQueueLock = new Object();
    /**
     * Name examples :
     * </p>
     * Executor __ SPLITTER
     * </p>
     * Executor __|__ PROCESS
     * </p>
     * Executor __|__|__ WRITER
     *
     * @param nbThreads
     * @param maxQueueSize
     * @param name
     */
    public FlowControlExecutor(int nbThreads, int maxQueueSize, final String name) {
        this.timeMilliStart = System.currentTimeMillis();
        this.nbTotalTasks = nbThreads + maxQueueSize;
        this.semaphore = new Semaphore(nbTotalTasks);
        this.name = name;
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
        executor.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, name + "_" + counterForName.incrementAndGet());
            }
        });
    }

    public void setThrowable(Throwable e) {
        executionExceptions.add(e);
        semaphore.release();
        synchronized (this) {
            if (semaphore.availablePermits() != nbTotalTasks) {
                return;
            }
            notify();
        }
    }

    public void aggregate(V elementToAggregate) {
        processAggregation(elementToAggregate);
        semaphore.release();
        synchronized (this) {
            if (!isQueueEmpty()) {
                return;
            }
            notify();
        }
        if (isQueueEmpty())
            synchronized (emptyQueueLock){
                emptyQueueLock.notifyAll();
            }
    }
    public boolean isQueueEmpty(){
        return semaphore.availablePermits() == nbTotalTasks;
    }

    protected void processAggregation(V elementToAggregate) {
    }

    public void submitWithException(Callable<V> callable) throws Throwable {
        submit(callable);
        if (executionExceptions.size() > 0) {
            throw executionExceptions.poll();
        }
    }


    public void submit(Callable<V> callable) throws InterruptedException {
        semaphore.acquire();
        callable.setExecutorWithFlowControl(this);
        executor.execute(callable);
    }
    public abstract boolean isSubmitsEnds();
    private void waitAndShutdownWithException(boolean throwException) throws Throwable {
        synchronized (this) {
            if (semaphore.availablePermits() != nbTotalTasks) {
                wait();
            }
        }
        executor.shutdown();
        if (throwException && executionExceptions.size() > 0) {
            throw executionExceptions.poll();
        }
    }
    public void waitAndShutdownWithException() throws Throwable {
        waitAndShutdownWithException(true);
    }

    public void waitAndShutdown() throws InterruptedException {
        try {
            waitAndShutdownWithException(false);
        } catch (Throwable e) {
            if (e instanceof InterruptedException)
                throw (InterruptedException)e;
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public int getNbTotalTasks() {
        return nbTotalTasks;
    }

    public Queue<Throwable> getExecutionExceptions() {
        return executionExceptions;
    }

    public String getName() {
        return name;
    }

    public long getNbTaskExecuted() {
        return executor.getTaskCount();
    }

    public BlockingQueue<Runnable> getQueue() {
        return executor.getQueue();
    }

    public static int getNbCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    public long getSpeedAvgNbTaskBySec(int chunkSize) {
        long speedAvgNbTaskBySec = (long) (((double) executor.getTaskCount()) / (((double) System.currentTimeMillis() - timeMilliStart) / 1000));
        return chunkSize * speedAvgNbTaskBySec;
    }

    public long getSpeedNbTaskBySec(int chunkSize) {
        long timeMilliCurr = System.currentTimeMillis();
        long nbTaskExecutedCurr = executor.getTaskCount();
        long speedNbTaskBySec = (long) (((double) nbTaskExecutedCurr - nbTaskExecutedLast) / (((double) timeMilliCurr - timeMilliLast) / 1000));
        timeMilliLast = timeMilliCurr;
        nbTaskExecutedLast = nbTaskExecutedCurr;
        return chunkSize * speedNbTaskBySec;
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public String toString() {
        return toString(1);
    }

    public String toString(long chunkSize) {
        long timeMilliCurr = System.currentTimeMillis();
        // long nbTaskExecutedCurr = executor.getTaskCount();
        // long nbTaskExecutedCurr = nbTotalTasks;
        int actives = executor.getActiveCount();
        int queue = nbTotalTasks - semaphore.availablePermits() - actives;
        long nbTaskExecutedCurr = executor.getTaskCount() - queue - actives;
        nbTaskExecutedCurr = nbTaskExecutedCurr < 0 ? 0 : nbTaskExecutedCurr;

        long speedNbTaskBySec = (long) (((double) nbTaskExecutedCurr - nbTaskExecutedLast) / (((double) timeMilliCurr - timeMilliLast) / 1000));
        long speedAvgNbTaskBySec = (long) (((double) nbTaskExecutedCurr) / (((double) timeMilliCurr - timeMilliStart) / 1000));

        timeMilliLast = timeMilliCurr;
        nbTaskExecutedLast = nbTaskExecutedCurr;

        String space = "               ";
        StringBuffer sb = new StringBuffer();
        sb.append("Thread ");
        sb.append(name);
        sb.append(" : Actives : ");
        sb.append((actives > 0 ? actives : 0) + space, 0, 2);
        sb.append(", Queue : ");
        sb.append((queue > 0 ? queue : 0) + space, 0, 4);
        sb.append(", Speed/Sec :");
        sb.append(" Current="+decimalFormat.format(speedNbTaskBySec * chunkSize) + space, 0, 20);
        sb.append(" - ");
        sb.append(" Avg="+decimalFormat.format(speedAvgNbTaskBySec * chunkSize) + space, 0, 20);
        sb.append(", Done : ");
        sb.append(decimalFormat.format(nbTaskExecutedCurr * chunkSize) + space, 0, 14);
        sb.append(", ");
        sb.append(((((double) timeMilliCurr) - timeMilliStart) / 1000) + space, 0, 8);
        sb.append(" Mem (Mb):");
        sb.append(" Aval="+decimalFormat.format(((runtime.totalMemory() - runtime.freeMemory()) / mb))+ space,0, 14);
        sb.append(", ");
        sb.append(" Free="+(runtime.freeMemory() / mb)+ space,0, 14);
        sb.append(", ");
        sb.append(" Total="+(runtime.totalMemory() / mb)+ space,0, 15);
        sb.append(", ");
        sb.append(" Max="+(runtime.maxMemory() / mb)+ space,0, 14);
        return sb.toString();
    }
    public void printLog(int period) {
        printLog(0, period);
    }

    public void printLog(int delay, int millisec) {
        printLog(delay, millisec, 1);
    }
    private long nbLinesClunk;
    public void printLog(int delay, int millisec, final long nbLinesClunk) {
        timer = new Timer(true);
        this.nbLinesClunk = nbLinesClunk;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                FlowControlExecutor.this.printLog(nbLinesClunk);
            }
        }, delay, millisec);
    }

    public void printLogStop() {
        if (timer != null) {
            if (executor.getActiveCount()>0)
                printLog(nbLinesClunk);
            timer.cancel();
        }
    }
    private void printLog(long nbLinesClunk){
        System.out.println(toString(nbLinesClunk));
    }
}