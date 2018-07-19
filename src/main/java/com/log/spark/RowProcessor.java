package com.log.spark;

import com.log.common.RegexMatch;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

import java.util.List;
import java.util.regex.Matcher;

public class RowProcessor implements Runnable {

    private List<Row> array;
    private String fileName;
    private String line;

    public RowProcessor(List<Row> array, String fileName, String line) {
        this.array = array;
        this.fileName = fileName;
        this.line = line;
    }

    @Override
    public void run() {
        Matcher matcher = RegexMatch.PatternFullLog.matcher(line);

        if (matcher.find()) {

            String timestamp = matcher.group("timestamp");
            String sev = matcher.group("sev");
            String tag = matcher.group("tag").trim();
            String message = matcher.group("message");

            array.add(RowFactory.create(fileName, timestamp, sev, tag, message));
        }
    }
}
