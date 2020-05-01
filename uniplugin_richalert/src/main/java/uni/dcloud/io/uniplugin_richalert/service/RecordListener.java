package uni.dcloud.io.uniplugin_richalert.service;

public interface RecordListener {
    void onStartRecord();
    void onPauseRecord();
    void onResumeRecord();
    void onStopRecord();
    void onRecording(String time);
}
