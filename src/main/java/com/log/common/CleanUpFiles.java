package com.log.common;

public class CleanUpFiles {

    private static CleanUpFiles instance;
    private String folderName;
    private String zipName;

    private CleanUpFiles(){}

    public static CleanUpFiles getInstance(){
        if(instance ==  null){
            instance = new CleanUpFiles();
        }

        return instance;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getZipName() {
        return zipName;
    }

    public void setZipName(String zipName) {
        this.zipName = zipName;
    }
}
