package com.adro.firedrive;

import com.google.firebase.Timestamp;

public class FileModel {
    private String id;
    private String fileName;
    private String fileUrl;
    private double size;
    private long sizeInKB;
    private String date;
    private String time;
    private Timestamp timestamp;

    public FileModel() {
    }

    public FileModel(String id, String fileName, String fileUrl, double size, long sizeInKB, String date, String time) {
        this.id = id;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.size = size;
        this.sizeInKB = sizeInKB;
        this.date = date;
        this.time = time;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public String getFileUrl() {
        return fileUrl;
    }
    public String getDate() {
        return date;
    }
    public String getTime() {
        return time;
    }


    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    public double getSize() {
        return size;
    }
    public void setSize(double size) {
        this.size = size;
    }
    public long getSizeInKB() {
        return sizeInKB;
    }
    public void setSizeInKB(long sizeInKB) {
        this.sizeInKB = sizeInKB;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public void setTime(String time) {
        this.time = time;
    }


    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
