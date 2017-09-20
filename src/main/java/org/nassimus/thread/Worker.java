package org.nassimus.thread;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
class Worker<V> implements java.lang.Runnable {
    private final FlowControlExecutor<V> executorWithFlowControl;
    private final Runnable runnable;
    public Worker(FlowControlExecutor<V> executorWithFlowControl, Runnable runnable){
        this.executorWithFlowControl = executorWithFlowControl;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } catch (Exception e) {
            executorWithFlowControl.pushException(e);
        } finally {
            executorWithFlowControl.releaseSemaphore();
        }
    }
}