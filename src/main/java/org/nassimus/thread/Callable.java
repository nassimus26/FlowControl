package org.nassimus.thread;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class Callable<V> implements Runnable {
    private FlowControlExecutor<V> executorWithFlowControl = null;

    public void setExecutorWithFlowControl(FlowControlExecutor<V> executorWithFlowControl) {
        this.executorWithFlowControl = executorWithFlowControl;
    }

    public FlowControlExecutor<V> getExecutorWithFlowControl() {
        return executorWithFlowControl;
    }

    @Override
    public void run() {
        try {
            call();
        } catch (Exception e) {
            executorWithFlowControl.pushException(e);
        } finally {
            executorWithFlowControl.release();
        }
    }

    public abstract void call() throws Exception;

}