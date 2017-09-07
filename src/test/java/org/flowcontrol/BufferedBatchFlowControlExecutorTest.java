package org.flowcontrol;

import org.junit.Assert;
import org.junit.Test;
import org.nassimus.thread.BufferedBatchFlowControlExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public class BufferedBatchFlowControlExecutorTest {

    @Test
    public void testFlowControl() throws Exception {
        int nbrRows = 2_000_000;
        final List<String> rows = new ArrayList<>();
        for (int i=0;i<nbrRows;i++)
            rows.add(generateRow(i));
        final List<String> expectedValues = new ArrayList<>();
        System.out.println("Starting 1 Thread processing...");
        long now = System.currentTimeMillis();
        for (String row : rows)
            expectedValues.add(transformRow(row));
        double processDurationWithOneThread = ((System.currentTimeMillis()-now)/1000.0);
        System.out.println("1 Thread processing takes "+ processDurationWithOneThread +" seconds");
        final List<String> result = new Vector<>();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        BufferedBatchFlowControlExecutor<String, String[]> processRows =
                new BufferedBatchFlowControlExecutor<>(
                        values -> {
                            ArrayList<String> tmp = new ArrayList<>();
                            for ( int i=0; i<values.length; i++ ) {
                                tmp.add(transformRow(values[i]));
                            }
                            result.addAll(tmp);
                        }, 1000, BufferedBatchFlowControlExecutor.getNbCores(), 500, "processRows") {

                    @Override
                    public void handleException(Exception e) {
                        /* The executor will throw the exception at the end if any exception */
                    }

                    @Override
                    public boolean isSubmitsEnds() {
                        return atomicBoolean.get();
                    }

                };

        System.out.println("Starting Parallel processing...");
        processRows.printLog(0, 100);
        now = System.currentTimeMillis();
        for (String row:rows){
            try {
                processRows.submit(row);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        atomicBoolean.set(true);
        processRows.waitAndFlushAndShutDown();
        double parallelDuration = ((System.currentTimeMillis()-now)/1000.0);
        System.out.println("Parallel processing takes "+ parallelDuration +
                " seconds ("+ (int)(processDurationWithOneThread*100/parallelDuration)+"% faster)");
        Collections.sort(expectedValues);
        Collections.sort(result);
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
