package com.chen.recordscreen.service;

public interface RecordListener {
    void onStartRecord();
    void onStopRecord(String path);
}
