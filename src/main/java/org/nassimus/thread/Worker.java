package org.nassimus.thread;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
class Worker<V> implements java.lang.Runnable {

    private final FlowControlExecutor<V> executorWithFlowControl;
    private final Callable callable;

    public Worker(FlowControlExecutor<V> executorWithFlowControl, Callable callable){
        this.executorWithFlowControl = executorWithFlowControl;
        this.callable = callable;
    }

    @Override
    public void run() {
        try {
            callable.run();
        } catch (Exception e) {
            executorWithFlowControl.pushException(e);
        } finally {
            executorWithFlowControl.releaseSemaphore();
        }
    }

}