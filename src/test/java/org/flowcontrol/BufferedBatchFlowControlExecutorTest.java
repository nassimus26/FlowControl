package org.flowcontrol;

import org.junit.Assert;
import org.junit.Test;
import org.nassimus.thread.BufferedBatchCallable;
import org.nassimus.thread.BufferedBatchFlowControlExecutor;

import java.text.DecimalFormat;
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
    public void testFlowControl() throws Exception {
        AtomicInteger count = new AtomicInteger();
        int nbrRows = 2_000_000;
        final List<String> expectedValues = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i=0;i<nbrRows;i++)
            expectedValues.add(transformRow(generateRow(i)));
        double processDurationWithOneThread = ((System.currentTimeMillis()-now)/1000.0);
        final AtomicInteger expectedCount = new AtomicInteger();
        final List<String> result = new Vector<>();
        BufferedBatchFlowControlExecutor<String, String[]> processRows =
                new BufferedBatchFlowControlExecutor<>(
                        values -> {
                            ArrayList<String> tmp = new ArrayList<>();
                            for ( int i=0; i<values.length; i++ ) {
                                tmp.add(transformRow(values[i]));
                            }
                            result.addAll(tmp);
                            return values;
                        }, 1000, BufferedBatchFlowControlExecutor.getNbCores(), 500, "processRows") {

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
        now = System.currentTimeMillis();
        while (count.get()<nbrRows){
            try {
                processRows.submit("row_"+count.get());
                count.incrementAndGet();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        processRows.waitAndFlushAndShutDown();
        double parallelDuration = ((System.currentTimeMillis()-now)/1000.0);
        System.out.println("Parallel processing done in "+ parallelDuration +
                " seconds, instead of "+processDurationWithOneThread+" seconds on 1 Thread ("+
                (int)(processDurationWithOneThread*100/parallelDuration)+"% faster)");
        Collections.sort(expectedValues);
        Collections.sort(result);
        Assert.assertEquals( nbrRows, expectedCount.get() );
        Assert.assertArrayEquals( expectedValues.toArray(), result.toArray() );
    }
    private String generateRow(int i){
        return "row_"+i;
    }

    private String transformRow(String row){// some CPU operations
        return row.replaceAll("row", "replaceMe").replaceAll("_", "!")
                .replaceAll("replaceMe", "ligne").replaceAll("!", "_");
    }

}
