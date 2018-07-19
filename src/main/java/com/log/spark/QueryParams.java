package com.log.spark;

import java.awt.*;
import java.util.HashMap;

public class QueryParams {

    private String fromDateStr;
    private String toDateStr;
    private String filename;
    private String tag;
    private String sev;
    private String message;

    private long initialIndex;
    private String searchTxtInFiles;

    private HashMap<String, Color> highlightWords = new HashMap<>();

    private static QueryParams instance;


    private QueryParams() {
    }

    public static QueryParams getInstance() {
        if (instance == null) {
            instance = new QueryParams();
        }
        return instance;
    }

    public void clearAll() {
        setFromDateStr(null);
        setToDateStr(null);
        setFilename(null);
        setTag(null);
        setSev(null);
        setMessage(null);
        highlightWords.clear();
    }

    public HashMap<String, Color> getHighlightWords() {
        return highlightWords;
    }

    public void setHighlightWords(HashMap<String, Color> highlightWords) {
        this.highlightWords = highlightWords;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getSev() {
        return sev;
    }

    public void setSev(String sev) {
        this.sev = sev;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFromDateStr() {
        return fromDateStr;
    }

    public void setFromDateStr(String fromDateStr) {
        this.fromDateStr = fromDateStr;
    }

    public String getToDateStr() {
        return toDateStr;
    }

    public void setToDateStr(String toDateStr) {
        this.toDateStr = toDateStr;
    }

    public void setInitialIndex(long initialIndex) {
        this.initialIndex = initialIndex;
    }

    public long getInitialIndex() {
        return initialIndex;
    }

    public String getSearchTxtInFiles() {
        return searchTxtInFiles;
    }

    public void setSearchTxtInFiles(String searchTxtInFiles) {
        this.searchTxtInFiles = searchTxtInFiles;
    }

    @Override
    public String toString() {
        return "QueryParams{" +
                "fromDateStr='" + fromDateStr + '\'' +
                ", toDateStr='" + toDateStr + '\'' +
                ", filename='" + filename + '\'' +
                ", tag='" + tag + '\'' +
                ", sev='" + sev + '\'' +
                ", message='" + message + '\'' +
                ", highlightTxt='" + highlightWords + '\'' +
                '}';
    }
}
