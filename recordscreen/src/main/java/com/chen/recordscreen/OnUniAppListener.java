package com.chen.recordscreen;

public interface OnUniAppListener {
    void onStartRecord(String caseNumber);
    void onPauseRecord();
    void onResumeRecord();
    void onStopRecord(String path);
}
