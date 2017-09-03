package org.nassimus.thread;

public abstract class Callable<V> implements Runnable {
    private FlowControlExecutor<V> executorWithFlowControl = null;

    public void setExecutorWithFlowControl(FlowControlExecutor<V> executorWithFlowControl) {
        this.executorWithFlowControl = executorWithFlowControl;
    }

    @Override
    public void run() {
        try {
            executorWithFlowControl.aggregate(call());
        } catch (Throwable e) {
            executorWithFlowControl.setThrowable(e);
        }
    }

    public abstract V call() throws Throwable;

}