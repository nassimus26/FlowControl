package org.nassimus.thread;

public abstract class Callable<V> implements Runnable {
    private ExecutorWithFlowControl<V> executorWithFlowControl = null;

    public void setExecutorWithFlowControl(ExecutorWithFlowControl<V> executorWithFlowControl) {
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