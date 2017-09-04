package org.flowcontrol;

import org.junit.Assert;
import org.junit.Test;
import org.nassimus.thread.BufferedBatchCallable;
import org.nassimus.thread.BufferedBatchFlowControlExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public class BufferedBatchFlowControlExecutorTest {

    @Test
    public void testFlowControl() throws Throwable {
        AtomicInteger count = new AtomicInteger();
        int nbrRows = 2_000_000;
        final List<String> expectedValues = new ArrayList<>();
        for (int i=0;i<nbrRows;i++)
            expectedValues.add(transformRow(generateRow(i)));
        final AtomicInteger expectedCount = new AtomicInteger();
        final List<String> result = new Vector<>();
        BufferedBatchFlowControlExecutor<String, String[]> processRows =
                new BufferedBatchFlowControlExecutor<>(
                        values -> {
                            for ( int i=0; i<values.length; i++ ) {
                                result.add(transformRow(values[i]));
                            }
                            return values;
                        }, 100, BufferedBatchFlowControlExecutor.getNbCores(), 5000, "processRows") {

                    @Override
                    public void handleException(Exception e) {
                        /* The executor will throw the exception at the end if any exception */
                    }

                    @Override
                    public boolean isSubmitsEnds() {
                        return count.get()==nbrRows;
                    }

                    @Override
                    public void processAggregation(String[] els) {
                        expectedCount.accumulateAndGet(els.length, (left, right) -> left+right );
                    }
                };

        System.out.println("Starting Parallel processing...");
        processRows.printLog(0, 100);
        long now = System.currentTimeMillis();
        while (count.get()<nbrRows){
            try {
                processRows.submit("row_"+count.get());
                count.incrementAndGet();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        processRows.waitAndFlushAndShutDown();
        System.out.println("Parallel processing done in "+((System.currentTimeMillis()-now)/1000.0)+" seconds");
        Collections.sort(expectedValues);
        Collections.sort(result);
        Assert.assertEquals( nbrRows, expectedCount.get() );
        Assert.assertArrayEquals( expectedValues.toArray(), result.toArray() );
    }
    private String generateRow(int i){
        return "row_"+i;
    }

    private String transformRow(String row){// some CPU operations
        return row.replaceAll("row", "replaceMe").replaceAll("_", " ")
                .replaceAll("replaceMe", "ligne");
    }

}
