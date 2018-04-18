package com.log.dbbatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

public class BalanceBatch {
    private final List<RunnableBatch> threads = new ArrayList<>();

    private Queue<String> queue = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 5000;

    public BalanceBatch(int nbThread) {
        IntStream.range(0, nbThread).mapToObj(i -> new RunnableBatch(BATCH_SIZE, queue)).forEach(threads::add);
    }

    public void send(String value) {
        queue.add(value);
    }

    public void startAll() {
        for (RunnableBatch t : threads) {
            new Thread(t).start();
        }
    }

    public void stopAll() {
        for (RunnableBatch t : threads) {
            t.stop();
        }
    }
}