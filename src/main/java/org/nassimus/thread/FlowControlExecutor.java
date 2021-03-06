package org.nassimus.thread;

import java.text.DecimalFormat;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public class FlowControlExecutor<V> {

    private static Runtime runtime = Runtime.getRuntime();
    private static int mb = 1024 * 1024;

    private DecimalFormat decimalFormat = new DecimalFormat();

    protected Queue<Exception> executionExceptions = new LinkedBlockingQueue<Exception>();

    protected Semaphore semaphore = null;
    private String name = null;
    protected ThreadPoolExecutor executor = null;
    protected int nbTotalTasks = 0;

    private long timeMilliStart;
    private long timeMilliLast;
    private long nbTaskExecutedLast;

    private Timer timer = null;
    protected Object emptyQueueLock = new Object();

    public FlowControlExecutor(int nbThreads, int maxQueueSize, final String name) {
        this(nbThreads, maxQueueSize, new ThreadFactory() {
            @Override
            public Thread newThread(java.lang.Runnable r) {
                return new Thread(r, name + "_" + new AtomicInteger().incrementAndGet());
            }
        });
        this.name = name;
    }

    public FlowControlExecutor(int nbThreads, int maxQueueSize, final ThreadFactory threadFactory) {
        this.timeMilliStart = System.currentTimeMillis();
        this.nbTotalTasks = nbThreads + maxQueueSize;
        this.semaphore = new Semaphore(nbTotalTasks);
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);
        this.executor.setThreadFactory(threadFactory);
    }

    public void handleException(Exception e) {
        e.printStackTrace();
    }

    void pushException(Exception e) {
        executionExceptions.add(e);
        handleException(e);
    }

    void releaseSemaphore() {
        semaphore.release();
        if (isQueueEmpty()) {
            synchronized (emptyQueueLock){
                emptyQueueLock.notifyAll();
            }
        }
    }

    public boolean isQueueEmpty(){
        return semaphore.availablePermits() == nbTotalTasks;
    }

    public void submit(Callable runnable) throws InterruptedException {
        Worker<V> worker = new Worker<V>(this, runnable);
        semaphore.acquire();
        executor.execute(worker);
    }



    protected void wait(boolean throwException, boolean shutdown) throws Exception {
        synchronized (emptyQueueLock) {
            if (!isQueueEmpty()){
                try {
                    emptyQueueLock.wait();
                }finally {
                    if (shutdown) {
                        executor.shutdown();
                        printLogStop();
                        if (throwException && executionExceptions.size() > 0)
                            throw executionExceptions.poll();
                    }
                }
            }
        }
    }

    public void waitWithException(boolean shutdown) throws Exception {
        wait(true, shutdown);
    }

    public void wait(boolean shutdown) throws InterruptedException {
        try {
            wait(false, shutdown);
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

    public Queue<Exception> getExecutionExceptions() {
        return executionExceptions;
    }

    public String getName() {
        return name;
    }

    public long getNbTaskExecuted() {
        return executor.getTaskCount();
    }

    public BlockingQueue<java.lang.Runnable> getQueue() {
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
        sb.append(" Aval="+decimalFormat.format(((runtime.totalMemory() - runtime.freeMemory()) / mb))+ space,0, 12);
        sb.append(", ");
        sb.append(" Free="+(runtime.freeMemory() / mb)+ space,0, 12);
        sb.append(", ");
        sb.append(" Total="+(runtime.totalMemory() / mb)+ space,0, 13);
        sb.append(", ");
        sb.append(" Max="+(runtime.maxMemory() / mb)+ space,0, 12);
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
            printLog(nbLinesClunk);
            timer.cancel();
        }
    }
    private void printLog(long nbLinesClunk){
        System.out.println(toString(nbLinesClunk));
    }
}