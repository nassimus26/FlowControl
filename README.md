# FlowControl

Simple but powerful Thread Pool Executor with Flow Control and BatchBuffer execution

**Features :** 

1_ Exceptions handling

2_ Fixed BackPresure Queue

3_ Fixed BufferedBatch processing 

Main classes :

**FlowContorlExecutor** 

**BufferedBatchFlowControlExecutor** exemple :
```java
@Test
public void testFlowControl() throws Throwable {
    AtomicInteger count = new AtomicInteger();
    int nbrRows = 2_000_000;
    final List<String> expectedValues = new ArrayList<>();
    for (int i=0;i<nbrRows;i++)
        expectedValues.add(transformRow(generateRow(i)));

    final List<String> result = new Vector<>();
    BufferedBatchFlowControlExecutor<String> processRows =
            new BufferedBatchFlowControlExecutor<String>(

                new BufferedBatchCallable<String>() {
                    @Override
                    public void call(Object[] values) throws Exception {
                        for (Object s: values)
                            result.add( transformRow((String)s) );
                    }
                }, 100, BufferedBatchFlowControlExecutor.getNbCores(), 5000, "processRows") {

                @Override
                public void handleException(Exception e) {/* The executor will throw the exception at the end */}

                @Override
                public boolean isSubmitsEnds() {
                    return count.get()==nbrRows;
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
    Assert.assertArrayEquals( expectedValues.toArray(), result.toArray());
}
private String generateRow(int i){
    return "row_"+i;
}

private String transformRow(String row){// some CPU operations
    return row.replaceAll("row", "replaceMe").replaceAll("_", " ")
            .replaceAll("replaceMe", "ligne");
}

```
