package com.log.spark;

import com.log.common.Constants;
import com.log.common.RegexMatch;
import com.log.common.Utility;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.storage.StorageLevel;
import org.apache.tika.Tika;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class SparkProcessor {

    private SparkSession spark;
    private SparkContext sparkContext;
    private SQLContext sqlContext;
    private Long rowcount;
    private Dataset<Row> logDataFrame;
    private static long index = 0;
    private QueryParams queryParams = QueryParams.getInstance();

    public SparkProcessor() {
        SparkSession spark = SparkSession.builder().appName("Simple Application")
                .config("spark.master", "local").getOrCreate();

        SparkContext sc = spark.sparkContext();
        sc.setLogLevel("ERROR");

        this.spark = spark;
        this.sparkContext = sc;
    }

    public static void main(String[] args) throws IOException {
        SparkProcessor app = new SparkProcessor();

        /*String[] afiles = {"/Users/ravi/data/logs/logcat.txt.1",
                "/Users/ravi/data/logs/logcat.txt.2",
                "/Users/ravi/data/logs/logcat.txt.3"};

        for (String file : afiles) {
            app.writeFileToSchema(file);
        }*/

        String aFolder = "/Users/ravi/data-2";

        app.writeFileToSchema(aFolder);
    }

    public void writeFileToSchema(String filePath) {

        if (new File(filePath).isDirectory()) {
            List<String> filepaths = new ArrayList<>();
            try (Stream<Path> paths = Files.walk(Paths.get(filePath))) {
                paths.filter(Files::isRegularFile)
                        .forEach(file -> {
                            String fileName = file.toString();
                            try {
                                if (new Tika().detect(new File(fileName)).contains("text")) {
                                    filepaths.add(fileName);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }

            String[] folder = filepaths.toArray(new String[filepaths.size()]);

            for (String file : folder) {
                writeToSchema(file); // folder with multiple files
            }
        } else {
            writeToSchema(filePath); // single file
        }

        executeLazyCommands();
    }

    public void writeToSchema(String filePath) {

        StructType schema = getSchema();
        JavaRDD<Row> rowRDD = loadFileToRDD(filePath);
        if (spark.catalog().tableExists("mylogs")) {

            Dataset<Row> logDataFrameTemp = spark.createDataFrame(rowRDD, schema);
            logDataFrameTemp.createOrReplaceTempView("temptable");
            //logDataFrameTemp.write().mode(SaveMode.Append).insertInto("mylogs");//exception
            logDataFrame = logDataFrame.union(logDataFrameTemp);
        } else {
            logDataFrame = spark.createDataFrame(rowRDD, schema);
        }

        logDataFrame.createOrReplaceTempView("mylogs");
    }

    public void executeLazyCommands() {
        System.out.println("execution started :" + new Date());
        //Dataset<Row> results = spark.sql("SELECT * FROM mylogs where filename LIKE '%file:/Users/ravi/data-2/logs/logcat.txt.15'  AND  message LIKE '%error%'  AND  (timestamp BETWEEN '03-26 15:00:00' AND '03-26 15:10:00') AND sev IN ('V', 'D', 'I', 'W', 'E', 'F')");
        /*Dataset<Row> results = spark.sql("SELECT * FROM mylogs");
        Row allrows = results.first();

        System.out.println("Row  :" + allrows);
        System.out.println("Row data :-----" + allrows.getString(1) + "-----");

        System.out.println("Resultset count " + results.count());*/

        Long count = logDataFrame.count();
        setRowcount(count);

        //searchAllFiles("Started Connected Room API Service");

        //System.out.println(count);
        //System.out.println("execution completed :" + new Date());
        sqlContext = logDataFrame.sqlContext();
    }

    public void truncateLogs() {
        if (sqlContext != null) {
            sqlContext.sql("DROP TABLE IF EXISTS mylogs").collect();
            logDataFrame = logDataFrame.filter(value -> false);
            Long count = logDataFrame.count();
        }
        setRowcount(0L);
    }

    public List<String[]> searchAllFiles(String text) {
        //System.out.println("Filter data filter started : " + new Date());

        List<String[]> allrows = new ArrayList<>();

        index = queryParams.getInitialIndex();

        Dataset<Row> filterData = logDataFrame.filter(line -> line.getString(5).contains(text)
                && line.getLong(0) > index);
        if (filterData.count() == 0) {
            return allrows;
        }

        index = filterData.first().getLong(0);

        allrows.addAll(readResultSet(logDataFrame.filter(line -> line.getLong(0) >= (index - Constants.searchCount)
                && line.getLong(0) <= (index + Constants.searchCount)).collectAsList()));

        queryParams.setInitialIndex(index);

        //System.out.println("Filter data filter ended : " + new Date());

        return allrows;
    }

    public List<Row> getTagList() {
        Dataset<Row> results = sqlContext.sql("SELECT distinct(tag) FROM mylogs");
        List<Row> allrows = results.collectAsList();

        return allrows;
    }

    public StructType getSchema() {

        String schemaString = "filename timestamp sev tag message";

        List<StructField> fields = new ArrayList<>();

        StructField indexField = DataTypes.createStructField("index", DataTypes.LongType, true);
        fields.add(indexField);

        for (String fieldName : schemaString.split(" ")) {
            StructField field = DataTypes.createStructField(fieldName, DataTypes.StringType, true);
            fields.add(field);
        }

        StructType schema = DataTypes.createStructType(fields);

        return schema;
    }

    public JavaRDD<Row> loadFileToRDD(String filePath) {
        JavaSparkContext javaSparkContext = new JavaSparkContext(sparkContext);
        JavaPairRDD<String, String> fileNameContentsRDD = javaSparkContext.wholeTextFiles(filePath, 5);

        JavaRDD<Row> rowRDD = fileNameContentsRDD.flatMap((FlatMapFunction<Tuple2<String, String>, Row>) fileNameContent -> {
            String fileName = fileNameContent._1();
            String content = fileNameContent._2();
            String[] lines = content.split("[\r\n]+");
            List<Row> array = new ArrayList<>(lines.length);
            //System.out.println("file name :" + fileName + "  content size :" + lines.length);
            //ExecutorService executor = Executors.newFixedThreadPool(5);
            for (String line : lines) {

                /*Runnable worker = new RowProcessor(array, fileName, line);
                executor.execute(worker);*/

                Matcher matcher = RegexMatch.PatternFullLog.matcher(line);

                if (matcher.find()) {

                    String timestamp = matcher.group("timestamp");
                    String sev = matcher.group("sev");
                    String tag = matcher.group("tag").trim();
                    String message = matcher.group("message");

                    array.add(RowFactory.create(++index, fileName, timestamp, sev, tag, message));
                }
            }
            /*executor.shutdown();
            while (!executor.isTerminated()) {
            }*/

            return array.iterator();
        });

        rowRDD.persist(StorageLevel.MEMORY_ONLY());

        return rowRDD;
    }

    /*public JavaRDD<Row> getRowRDD(String filePath) {

        JavaRDD<String> logRDD = sparkContext.textFile(filePath, 5).toJavaRDD();

        RegexMatch reg = new RegexMatch();
        JavaRDD<Row> rowRDD = logRDD
                .filter(line -> reg.isMatched(line))
                .map((Function<String, Row>) line -> {

                    Matcher matcher = RegexMatch.PatternFullLog.matcher(line);
                    matcher.find();

                    String timestamp = matcher.group("timestamp");
                    String sev = matcher.group("sev");
                    String tag = matcher.group("tag").trim();
                    String message = matcher.group("message");

                    return RowFactory.create(filePath, timestamp, sev, tag, message);
                });

        rowRDD.persist(StorageLevel.MEMORY_ONLY());

        return rowRDD;
    }*/

    private java.util.List<String[]> readResultSet(List<Row> rs) {
        java.util.List<String[]> allLines = new ArrayList<>();
        //System.out.println("generating list started...." + new Date());
        rs.forEach(row -> {
            allLines.add(new String[]{
                    row.getString(2),
                    row.getString(3),
                    row.getString(4),
                    row.getString(5)});
        });
        return allLines;
    }

    public List<Row> getFilteredByFileName(String message) {
        //System.out.println("searching by filename :" + new Date());

        RelationalGroupedDataset values = logDataFrame.filter("message LIKE '%" + message + "%'").groupBy("filename");
        Dataset<Row> results = values.count();

        /*Dataset<Row> results = sqlContext.sql("SELECT filename, COUNT(filename) FROM mylogs where message LIKE '%" + message + "%' " +
                " group by filename ");*/
        List<Row> allrows = results.collectAsList();
        //System.out.println("end searching by filename :" + new Date());
        return allrows;
    }

    public List<String[]> getAllRowsFilteredByFileName(String message) {
        List<Row> allrows = logDataFrame.filter("message LIKE '%" + message + "%' ").collectAsList();

        return readResultSet(allrows);
    }

    public List<String[]> getFilteredByTag(String tag) {
        return getFilteredByTagAndSev(tag, null);
    }

    public List<String[]> getFilteredByTag(String[] sevValues) {
        return getFilteredByTagAndSev(null, sevValues);
    }

    public List<String[]> getFilteredByTextAndTagAndSev(String txt, String tag, String[] sev) {

        String qry = " message LIKE '%" + txt + "%' ";
        qry = qry + getFilteredQuery(tag, sev);

        List<Row> allrows = logDataFrame.filter(qry).collectAsList();

        return readResultSet(allrows);
    }

    public List<String[]> getFilteredByTagAndSev(String tag, String[] sev) {

        String qry = getFilteredQuery(tag, sev);

        List<Row> allrows = logDataFrame.filter(qry).collectAsList();

        return readResultSet(allrows);
    }

    private StringJoiner getArray(String[] sev) {

        StringJoiner joiner = new StringJoiner("','", "'", "'");
        for (String st : sev) {
            joiner.add(st);
        }

        return joiner;
    }

    private String getFilteredQuery(String tag, String[] sev) {

        String qry = "";

        if (tag != null && sev != null) {
            qry = " tag LIKE '%" + tag + "%' AND sev IN (" + getArray(sev) + ")";
        } else {
            if (tag != null) {
                qry = " tag LIKE '%" + tag + "%'";
            }
            if (sev != null) {
                qry = " sev IN (" + getArray(sev) + ")";
            }
        }

        return qry;
    }

    public Long getRowcount() {
        return rowcount;
    }

    public void setRowcount(Long rowcount) {
        this.rowcount = rowcount;
    }

    public List<String[]> getFilteredData(String message, String filename) {
        Dataset<Row> results = sqlContext.sql("SELECT * FROM mylogs WHERE " +
                " message LIKE '%" + message + "%' and filename LIKE '%" + filename + "' ");
        List<Row> allrows = results.collectAsList();

        return readResultSet(allrows);
    }

    public List<String[]> getFilteredData() {

        QueryParams queryParams = QueryParams.getInstance();

        String qry = StringUtils.EMPTY;
        List<Row> allrows = new ArrayList<>();

        String fromDate = queryParams.getFromDateStr();
        String toDate = queryParams.getToDateStr();
        String msg = queryParams.getMessage();
        String tag = queryParams.getTag();
        String file = queryParams.getFilename();
        String[] sev = Utility.getSevValues(Constants.sev, queryParams.getSev());

        if (StringUtils.isNotBlank(file)) {
            qry = " filename LIKE '%" + file + "' ";
        }

        if (StringUtils.isNotBlank(tag) && !tag.equals("search tag")) {
            qry = and(qry);
            qry = qry + " tag LIKE '%" + tag + "%' ";
        }

        if (StringUtils.isNotBlank(msg) && !msg.equals("search message")) {
            qry = and(qry);
            qry = qry + " " + likeOR(msg) + " ";
        }

        if (StringUtils.isNotBlank(fromDate) && StringUtils.isNotBlank(toDate)) {
            qry = and(qry);
            qry = qry + " timestamp BETWEEN '" + fromDate + "' AND '" + toDate + "'";
        }

        qry = and(qry);
        if (StringUtils.isNotBlank(qry)) {
            qry = qry + "sev IN (" + getArray(sev) + ")";
            allrows = logDataFrame.filter(qry).collectAsList();
        } else {
            qry = " sev IN (" + getArray(sev) + ")";
            allrows = logDataFrame.filter(qry).limit(1000).collectAsList();
        }

        //System.out.println("Selected Parameters : " + qry);
        //System.out.println("got data from dataframe :" + allrows.size());

        return readResultSet(allrows);
    }

    public String likeOR(String str) {
        String qry = "";

        //String s = "ravi";
        String[] sev = str.split(",");

        String prefix = "message LIKE '%";
        String suffix = "%' OR ";

        for (String st : sev) {
            qry = qry + prefix + st.trim() + suffix;
        }

        qry = qry.substring(0, qry.lastIndexOf(" OR"));

        qry = "(" + qry + ")";

        return qry;
    }

    public String and(String qry) {

        if (StringUtils.isNotBlank(qry)) {
            qry = qry + " AND ";
        }

        return qry;
    }

}
