package com.log.dbbatch;

import java.util.Queue;

public class RunnableBatch implements Runnable {

    private boolean started = true;
    private Queue<String> queue;
    private int batchLimit;

    public RunnableBatch(int batchLimit, Queue<String> queue) {
        this.batchLimit = batchLimit;
        this.queue = queue;
    }

    @Override
    public void run() {
        try (BatchInsert batch = new BatchInsert(batchLimit)) {
            while (!queue.isEmpty() || started) {
                String line = queue.poll();
                if (line == null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {

                    }
                } else {
                    String[] values = line.split(" ");
                    String msg = line.substring(line.indexOf(values[5]));
                    batch.insert(values[0] + " " + values[1], values[4], values[5], msg);
                }
            }
        }
    }

    public void stop() {
        started = false;
    }
}