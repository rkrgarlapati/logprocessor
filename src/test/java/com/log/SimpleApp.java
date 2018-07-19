package com.log;

import com.log.common.RegexMatch;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;

public class SimpleApp {

    public static void main(String[] args) {
        SimpleApp app = new SimpleApp();
        SparkSession spark = SparkSession.builder().appName("Simple Application")
                .config("spark.master", "local").getOrCreate();
        //app.readFile(spark, "/Users/ravi/data/logs/logcat.txt.3");
        app.readFileToSchema(spark, "/Users/ravi/data-2/logs");
    }

    public void readFileToSchema(SparkSession spark, String filePath) {

        SparkContext sc = spark.sparkContext();
        JavaRDD<String> logRDD = sc.textFile(filePath, 1).toJavaRDD();

        String schemaString = "timestamp sev tag message";

        List<StructField> fields = new ArrayList<>();
        for (String fieldName : schemaString.split(" ")) {
            StructField field = DataTypes.createStructField(fieldName, DataTypes.StringType, true);
            fields.add(field);
        }
        StructType schema = DataTypes.createStructType(fields);

        RegexMatch reg = new RegexMatch();
        JavaRDD<Row> rowRDD = logRDD
                .filter(line -> reg.isMatched(line))
                .map((Function<String, Row>) line -> {

                    String[] sp = line.split(" ");
                    String msg = line.substring(line.indexOf(sp[5]));
                    return RowFactory.create(sp[0] + " " + sp[1], sp[4], sp[5], msg);
                });

        Dataset<Row> logDataFrame = spark.createDataFrame(rowRDD, schema);

        logDataFrame.createOrReplaceTempView("allLogs");

        Dataset<Row> results = spark.sql("SELECT distinct(tag) FROM allLogs");

        List<Row> allrows = results.collectAsList();

        System.out.println("size : " + allrows.size());

        //spark.stop();
    }

    /*public void readFile(SparkSession spark, String filePath) {
        //String logFile = "/Users/ravi/data/logs/logcat.txt.3"; // Should be some file on your system

        Dataset<String> logData = spark.read().textFile(filePath).cache();
        List<String> errorList = logData.filter(s -> s.contains("error")).collectAsList();
        System.out.println("error size :" + errorList.size());

        //spark.stop();
    }
*/

}
