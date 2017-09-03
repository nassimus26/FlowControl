package org.flowcontrol;

import org.junit.Assert;
import org.junit.Test;
import org.nassimus.thread.BufferedFlowControlExecutor;
import org.nassimus.thread.BuffredCallable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public class BufferedFlowControlExecutorTest {

    @Test
    public void testFlowControl() throws Throwable {
        AtomicInteger count = new AtomicInteger();
        int size = 2_000_000;
        final List<String> values = new ArrayList<>();
        for (int i=0;i<size;i++)
            values.add(transformRow(generateRow(i)));

        final List<String> result = new Vector<>();
        BufferedFlowControlExecutor<String> processRows =
                new BufferedFlowControlExecutor<String>(
                    new BuffredCallable<String>() {
                        @Override
                        public void call(Object[] values) throws Throwable {
                            for (Object s: values)
                                result.add( transformRow((String)s) );
                        }
                    }, 100, BufferedFlowControlExecutor.getNbCores(), 5000, "processRows") {
                        @Override
                        public boolean isWorkDone() {
                            return count.get()==size;
                        }
        };
        System.out.println("Starting Parallel processing");
        processRows.printLog(0, 100);
        long now = System.currentTimeMillis();
        while (count.get()<size){
            try {
                processRows.submit("row_"+count.get());
                count.incrementAndGet();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        processRows.waitAndFlushAndShutDown();
        System.out.println("Parallel processing done in "+((System.currentTimeMillis()-now)/1000.0)+" seconds");
        Collections.sort(values);
        Collections.sort(result);
        processRows.toString(1);
        Assert.assertArrayEquals( values.toArray(), result.toArray());
    }
    private String generateRow(int i){
        return "row_"+i;
    }

    private String transformRow(String row){// some CPU operations
        return row.replaceAll("row", "replaceMe").replaceAll("_", " ")
                .replaceAll("replaceMe", "ligne");
    }

}
