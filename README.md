# FlowControl

Simple but powerful Thread Pool Executor with Flow Control and BatchBuffer execution

Main classes :

**FlowContorlExecutor** 

**BufferedFlowContorlExecutor** exemple :
```java
@Test
    public void testFlowControl() throws InterruptedException {
        AtomicInteger count = new AtomicInteger();
        int size = 1000000;
        final List<String> values = new ArrayList<>();
        for (int i=0;i<size;i++) {
            values.add(transformRow(generateRow(i)));
        }
        final List<String> result = new Vector<>();
        BufferedFlowControlExecutor<String> processRows = new BufferedFlowControlExecutor<String>(new BuffredCallable<String>() {
                @Override
                public void call(Object[] values) throws Throwable {
                    for (Object s: values)
                        result.add( transformRow((String)s) );
                }
            }, 100, 4, 20, "processRows") {
            @Override
            public boolean isWorkDone() {
                return count.get()==size;
            }
        };
        while (count.get()<size){
            try {
                processRows.submit("row_"+count.get());
                count.incrementAndGet();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("waiting");
        processRows.waitAndFlushAndShutDown();
        Collections.sort(values);
        Collections.sort(result);
        Assert.assertArrayEquals( values.toArray(), result.toArray());
    }

```
