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

    @Override
    public void run() {
        try {
            executorWithFlowControl.aggregate(call());
        } catch (Exception e) {
            executorWithFlowControl.pushException(e);
        }
    }

    public abstract V call() throws Exception;

}