# FlowControl

Simple but powerful Thread Pool Executor with Flow Control and BatchBuffer execution

**Features :** 

- Threads Exceptions handling

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
    final List<String> rows = new ArrayList<>();
    for (int i=0;i<nbrRows;i++)
        rows.add(generateRow(i));
    final List<String> expectedValues = new ArrayList<>();
    long now = System.currentTimeMillis();
    for (String row : rows)
        expectedValues.add(transformRow(row));
    double processDurationWithOneThread = ((System.currentTimeMillis()-now)/1000.0);

    final List<String> result = new Vector<>();
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
                    return count.get()==nbrRows;
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
    count.set(rows.size());
    processRows.waitAndFlushAndShutDown();
    double parallelDuration = ((System.currentTimeMillis()-now)/1000.0);
    System.out.println("Parallel processing done in "+ parallelDuration +
            " seconds, instead of "+processDurationWithOneThread+" seconds on 1 Thread ("+
            (int)(processDurationWithOneThread*100/parallelDuration)+"% faster)");
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

```
