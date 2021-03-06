package org.flowcontrol;

import org.junit.Assert;
import org.junit.Test;
import org.nassimus.thread.BufferedBatchCallable;
import org.nassimus.thread.BufferedBatchFlowControlExecutor;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/*
* @author : Nassim MOUALEK
* cd_boite@yahoo.fr
* */
public class BufferedBatchFlowControlExecutorTest {

    @Test
    public void testFlowControl() throws Throwable {
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
        BufferedBatchFlowControlExecutor<String> processRows =
                new BufferedBatchFlowControlExecutor<String>(
                        new BufferedBatchCallable<String>() {
                            @Override
                            public void call(List<String> batchValues) {
                                ArrayList<String> tmp = new ArrayList<>();
                                for ( int i=0; i<batchValues.size(); i++ )
                                    tmp.add(transformRow(batchValues.get(i)));
                                result.addAll(tmp);
                            }
                        }, 1000, BufferedBatchFlowControlExecutor.getNbCores(), 50, "processRows") {

                    @Override
                    public void handleException(Exception e) {
                        /* The executor will throw the exception at the end if any exception */
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
        processRows.terminateWorks();
        processRows.waitAndFlushWithException(true);
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
