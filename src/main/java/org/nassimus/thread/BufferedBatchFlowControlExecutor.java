package org.nassimus.thread;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public abstract class BufferedBatchFlowControlExecutor<Type, ArrayOfType> extends FlowControlExecutor<ArrayOfType> {

    private BufferedBatchCallable<Type> callable;
    private List<Type> buffer;
    private int bufferSize;
    private final Class type;
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
    public BufferedBatchFlowControlExecutor(BufferedBatchCallable<Type> callable, int bufferSize, int nbThreads, int maxQueueSize, final String name) {
        super(nbThreads, maxQueueSize, name);
        this.bufferSize = bufferSize;
        this.callable = callable;
        this.buffer = new ArrayList<>();
        this.type = getGenericType();
    }

    public BufferedBatchFlowControlExecutor(int nbThreads, int maxQueueSize, final String name) {
        this(null,0, nbThreads, maxQueueSize, name );
    }
    private Class getGenericType(){
        Class clz = getClass();
        java.lang.reflect.Type genericType;
        while((genericType = clz.getGenericSuperclass())!=null &&
                !(ParameterizedType.class.isAssignableFrom(genericType.getClass()))){
            clz = clz.getSuperclass();
        }
        return (Class)((ParameterizedType) clz.getGenericSuperclass()).getActualTypeArguments()[0];
    }
    public void submitWithException(Type params) throws Throwable {
        submit(params);
        if (executionExceptions.size() > 0) {
            throw executionExceptions.poll();
        }
    }

    public void submit(Type params) throws InterruptedException {
        if (isSubmitsEnds())
            throw new RuntimeException("No more task accepted");
        synchronized (buffer){
            buffer.add(params);
            if (buffer.size()==bufferSize){
                process();
            }
        }
    }
    public abstract boolean isSubmitsEnds();

    private void process() throws InterruptedException{
        synchronized (buffer){
            if (buffer.isEmpty())
                return;
            final Type[] vals = (Type[]) buffer.toArray();
            Type[] valsWithRightType = (Type[]) Array.newInstance(type, vals.length);
            for (int i=0;i<vals.length;i++)
                valsWithRightType[i] = vals[i];
            buffer.clear();
            Callable<ArrayOfType> newCall = new Callable() {
                @Override
                public void call() throws Exception {
                    callable.call(valsWithRightType);
                }
            };
            submit(newCall);
        }
    }

    private boolean shouldFlush(){
        return isSubmitsEnds() && !buffer.isEmpty();
    }

    public void waitAndFlushAndShutDown() throws InterruptedException {
        try {
            waitAndFlushAndShutDownWithException(false);
        } catch (Throwable e) {
            if (e instanceof InterruptedException)
                throw (InterruptedException)e;
        }
    }
    public void waitAndFlushAndShutDownWithException() throws Throwable {
        waitAndFlushAndShutDownWithException(true);
    }
    private void waitAndFlushAndShutDownWithException(boolean throwException) throws Exception {
        while(true){
            synchronized (emptyQueueLock) {
                try {
                    if (!isQueueEmpty())
                        emptyQueueLock.wait();
                    if (shouldFlush())
                        process();
                    if (throwException && executionExceptions.size() > 0) {
                        throw executionExceptions.poll();
                    }
                } finally {
                    if (shouldFlush()) {
                        process();
                    } else if ( isSubmitsEnds() && isQueueEmpty() ) {
                        break;
                    }
                }
            }
        }
        //executor.shutdown();
        printLogStop();
    }

    @Override
    public String toString(long chunkSize) {
        return super.toString(chunkSize*bufferSize);
    }
}