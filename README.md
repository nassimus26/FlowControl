# FlowControl

Tiny but powerful Thread Pool Executor with Flow Control and BatchBuffer execution

**Features :** 

- Threads Exceptions handling

- Fixed Queue Size for BackPresure control

- Fixed BufferedBatch processing :
        
     * Will submit the callable when the buffer is ready (full), extremely useful to reduce Threads context switching

Main classes :

- **FlowControlExecutor** 

- **BufferedBatchFlowControlExecutor** 

**Maven Dependency**
```java
<dependency>
    <groupId>io.github.nassimus26</groupId>
    <artifactId>FlowControl</artifactId>
    <version>1.0.8</version> 
</dependency>
```    

Example :
```java
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
    final AtomicBoolean isProcessingEnds = new AtomicBoolean();
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

                @Override
                public boolean isSubmitsEnds() {
                    return isProcessingEnds.get();
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
    isProcessingEnds.set(true);
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
```

**Expected output**
```Starting 1 Thread processing...
1 Thread processing takes 4.686 seconds
Starting Parallel processing...
Thread processRows : Actives : 0 , Queue : 0   , Speed/Sec : Current=0           -  Avg=0              , Done : 0             , 0.003    Mem (Mb): Aval=1 017 ,  Free=1562  ,  Total=2580  ,  Max=4062   
Thread processRows : Actives : 8 , Queue : 26  , Speed/Sec : Current=653 000     -  Avg=643 000        , Done : 132 000       , 0.205    Mem (Mb): Aval=304   ,  Free=2275  ,  Total=2580  ,  Max=4062   
Thread processRows : Actives : 8 , Queue : 21  , Speed/Sec : Current=2 165 000   -  Avg=1 190 000      , Done : 381 000       , 0.32     Mem (Mb): Aval=1 277 ,  Free=1302  ,  Total=2580  ,  Max=4062   
Thread processRows : Actives : 8 , Queue : 18  , Speed/Sec : Current=693 000     -  Avg=1 034 000      , Done : 483 000       , 0.467    Mem (Mb): Aval=327   ,  Free=2252  ,  Total=2580  ,  Max=4062   
Thread processRows : Actives : 8 , Queue : 21  , Speed/Sec : Current=2 078 000   -  Avg=1 221 000      , Done : 695 000       , 0.569    Mem (Mb): Aval=1 166 ,  Free=1413  ,  Total=2580  ,  Max=4062   
Thread processRows : Actives : 8 , Queue : 3   , Speed/Sec : Current=952 000     -  Avg=1 160 000      , Done : 854 000       , 0.736    Mem (Mb): Aval=364   ,  Free=2215  ,  Total=2580  ,  Max=4062   
Thread processRows : Actives : 8 , Queue : 23  , Speed/Sec : Current=2 363 000   -  Avg=1 316 000      , Done : 1 114 000     , 0.846    Mem (Mb): Aval=1 408 ,  Free=1171  ,  Total=2580  ,  Max=4062   
Thread processRows : Actives : 8 , Queue : 20  , Speed/Sec : Current=575 000     -  Avg=1 191 000      , Done : 1 213 000     , 1.018    Mem (Mb): Aval=380   ,  Free=2547  ,  Total=2928  ,  Max=4062   
Thread processRows : Actives : 6 , Queue : 0   , Speed/Sec : Current=953 000     -  Avg=1 168 000      , Done : 1 315 000     , 1.125    Mem (Mb): Aval=798   ,  Free=2130  ,  Total=2928  ,  Max=4062   
Thread processRows : Actives : 8 , Queue : 19  , Speed/Sec : Current=2 355 000   -  Avg=1 269 000      , Done : 1 560 000     , 1.229    Mem (Mb): Aval=1 784 ,  Free=1144  ,  Total=2928  ,  Max=4062   
Thread processRows : Actives : 6 , Queue : 0   , Speed/Sec : Current=437 000     -  Avg=1 185 000      , Done : 1 620 000     , 1.366    Mem (Mb): Aval=401   ,  Free=2526  ,  Total=2928  ,  Max=4062   
Thread processRows : Actives : 8 , Queue : 20  , Speed/Sec : Current=2 152 000   -  Avg=1 254 000      , Done : 1 846 000     , 1.471    Mem (Mb): Aval=1 320 ,  Free=1608  ,  Total=2928  ,  Max=4062   
Thread processRows : Actives : 0 , Queue : 0   , Speed/Sec : Current=2 406 000   -  Avg=1 302 000      , Done : 2 000 000     , 1.535    Mem (Mb): Aval=1 923 ,  Free=1004  ,  Total=2928  ,  Max=4062   
Parallel processing takes 1.534 seconds (305% faster)
```