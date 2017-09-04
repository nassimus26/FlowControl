# FlowControl

Simple but powerful Thread Pool Executor with Flow Control and BatchBuffer execution

**Features :** 

- Exceptions handling

- Result Aggregation

- Fixed Blocking Queue for BackPresure 

- Fixed BufferedBatch processing :
        
     * Will submit the callable when the buffer is ready (full), extremely useful for small operations)

Main classes :

- **FlowContorlExecutor** 

- **BufferedBatchFlowControlExecutor** 

Example :
```java
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
                        for ( int i=0; i<values.length; i++ ) {
                            result.add(transformRow(values[i]));
                        }
                        return values;
                    }, 2000, BufferedBatchFlowControlExecutor.getNbCores(), 1000, "processRows") {

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

```
