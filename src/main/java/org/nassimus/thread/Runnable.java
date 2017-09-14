package org.nassimus.thread;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
class Runnable<V> implements java.lang.Runnable {
    private final FlowControlExecutor<V> executorWithFlowControl;
    private final Callable callable;
    public Runnable(FlowControlExecutor<V> executorWithFlowControl, Callable callable){
        this.executorWithFlowControl = executorWithFlowControl;
        this.callable = callable;
    }

    @Override
    public void run() {
        try {
            callable.call();
        } catch (Exception e) {
            executorWithFlowControl.pushException(e);
        } finally {
            executorWithFlowControl.releaseSemaphore();
        }
    }
}