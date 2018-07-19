package com.log.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBConnection {

    private static DBConnection instance;
    private Connection conn = null;
    private Statement stmt;
    private PreparedStatement preparedStmt;
    static String sql = "INSERT INTO DATA (TIMESTAMP, sev, tag, message)" +
            " VALUES (?,?,?,?)";

    private String qry = "SELECT * FROM DATA WHERE ";

    private DBConnection() {
        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:~/test;LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0");
            conn.setAutoCommit(false);
            createTable();
            preparedStmt = conn.prepareStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }

        return instance;
    }

    public Connection connection() {
        return conn;
    }

    private void createTable() {
        try {

            stmt = connection().createStatement();

            stmt.executeUpdate("DROP TABLE IF EXISTS DATA");
            stmt.executeUpdate("CREATE TABLE DATA(" +
                    "TIMESTAMP VARCHAR(50) NOT NULL, " +
                    "sev VARCHAR NOT NULL, " +
                    "tag VARCHAR(100) NOT NULL, " +
                    "message VARCHAR(5000) NOT NULL)");

            //stmt.executeUpdate("CREATE INDEX sev_indx ON DATA(sev)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertData(String timestamp, String sev, String tag, String message) {

        try {
            preparedStmt.setString(1, timestamp);
            preparedStmt.setString(2, sev);
            preparedStmt.setString(3, tag);
            preparedStmt.setString(4, message);

            preparedStmt.addBatch();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void executeBatch() {
        try {
            preparedStmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String[]> readData(String qryStr) {

        ResultSet rs;
        java.util.List<String[]> allLines = new ArrayList<>();
        try {
            if (qryStr != null && !qryStr.isEmpty()) {
                String qry = "SELECT * FROM DATA WHERE message LIKE '%" + qryStr + "%'";
                rs = stmt.executeQuery(qry);
            } else {
                rs = stmt.executeQuery("SELECT * FROM DATA");
            }

            allLines = readResultSet(rs);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allLines;
    }

    public List<String> getTagIndexData() {
        java.util.List<String> allLines = new ArrayList<>();
        try {

            ResultSet rs = stmt.executeQuery("SELECT tag,COUNT(tag) COUNT FROM DATA GROUP BY tag;");
            while (rs.next()) {
                allLines.add(
                        rs.getString("tag") +
                                " (" + rs.getString("count") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allLines;
    }

    public List<String> getSevIndexData() {
        java.util.List<String> sev = new ArrayList<>();
        try {

            ResultSet rs = stmt.executeQuery("SELECT DISTINCT sev FROM DATA;");
            while (rs.next()) {
                sev.add(rs.getString("sev"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sev;
    }

    public List<String[]> readSelectedMsgValues(String selectedRow) {
        ResultSet rs;
        java.util.List<String[]> allLines = new ArrayList<>();
        try {
            if (selectedRow != null && !selectedRow.isEmpty()) {
                String qry = "SELECT * FROM DATA WHERE message LIKE '%" + selectedRow + "%'";
                rs = stmt.executeQuery(qry);
            } else {
                rs = stmt.executeQuery("SELECT * FROM DATA");
            }

            allLines = readResultSet(rs);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allLines;
    }

    public List<String[]> readSelectedSev(String[] sev) {
        return readSelectedIndexValues(null, sev);
    }

    public List<String[]> readSelectedTag(String tag) {
        return readSelectedIndexValues(tag, null);
    }

    public List<String[]> readSelectedIndexValues(String tag, String[] sev) {

        java.util.List<String[]> allLines = new ArrayList<>();

        try {


            if(tag != null && sev != null) {
                qry = qry+" tag LIKE '%" + tag + "%' AND array_contains(?, sev)";
            } else {
                if(tag != null){
                    qry = qry + " tag LIKE '%" + tag + "%'";
                }
                if(sev != null){
                    qry = qry + " array_contains(?, sev)";
                }
            }

            System.out.println("Selected Parameters :"+qry);

            PreparedStatement prep = conn.prepareStatement(qry);
            if(sev != null) {
                prep.setObject(1, conn.createArrayOf("varchar", sev));
            }
            ResultSet rs = prep.executeQuery();

            allLines = readResultSet(rs);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allLines;
    }

    private java.util.List<String[]> readResultSet(ResultSet rs) throws SQLException {
        java.util.List<String[]> allLines = new ArrayList<>();

        while (rs.next()) {
            allLines.add(new String[]{
                    rs.getString("timestamp"),
                    rs.getString("sev"),
                    rs.getString("tag"),
                    rs.getString("message")});
        }

        rs.close();

        return allLines;
    }

    public void deleteAllRecords(){
        createTable();
    }
}
