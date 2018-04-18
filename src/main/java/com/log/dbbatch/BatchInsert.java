package com.log.dbbatch;

import com.log.db.DBConnection;

public class BatchInsert implements AutoCloseable {

    private int batchSize = 0;
    private final int batchLimit;

    final static DBConnection db = DBConnection.getInstance();

    public BatchInsert(int batchLimit) {
        this.batchLimit = batchLimit;
    }

    public void insert(String timestamp, String sev, String tag, String message) {

        db.insertData(timestamp, sev, tag, message);

        if (++batchSize >= batchLimit) {
            sendBatch();
        }
    }

    public void sendBatch() {
        db.executeBatch();
        System.out.format("Send batch with %d records%n", batchSize);
        batchSize = 0;
    }

    @Override
    public void close() {
        if (batchSize != 0) {
            sendBatch();
        }
    }
}