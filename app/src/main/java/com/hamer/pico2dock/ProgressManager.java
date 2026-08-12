package com.hamer.pico2dock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ProgressManager {
    private static ProgressManager instance;
    private final MutableLiveData<ProcessUpdate> updateLiveData = new MutableLiveData<>();
    private final MutableLiveData<String[]> fileListLiveData = new MutableLiveData<>();
    private final MutableLiveData<String[]> outputFileListLiveData = new MutableLiveData<>();
    
    private ProcessUpdate currentState = new ProcessUpdate(null, 0, null, false, false);
    private String[] apkFiles;
    private String[] apkFilesOut;

    private ProgressManager() {}

    public static synchronized ProgressManager getInstance() {
        if (instance == null) {
            instance = new ProgressManager();
        }
        return instance;
    }

    public LiveData<ProcessUpdate> getUpdateLiveData() {
        return updateLiveData;
    }

    public synchronized void postUpdate(ProcessUpdate update) {
        String status = update.statusText != null ? update.statusText : currentState.statusText;
        int progress = update.progress != -1 ? update.progress : currentState.progress;
        String error = update.errorMessage != null ? update.errorMessage : currentState.errorMessage;
        boolean finished = update.isFinished;
        boolean cancelled = update.isCancelled;

        currentState = new ProcessUpdate(status, progress, error, finished, cancelled);
        updateLiveData.postValue(currentState);
    }

    public void setProgress(int progress) {
        postUpdate(new ProcessUpdate(null, progress, null, false, false));
    }

    public void incrementProgress(int delta) {
        synchronized (this) {
            int newProgress = currentState.progress + delta;
            if (newProgress > 100) newProgress = 100;
            postUpdate(new ProcessUpdate(null, newProgress, null, false, false));
        }
    }

    public synchronized int getCurrentProgress() {
        return currentState.progress;
    }

    public synchronized void reset() {
        currentState = new ProcessUpdate(null, 0, null, false, false);
        updateLiveData.postValue(currentState);
    }

    public LiveData<String[]> getFileListLiveData() {
        return fileListLiveData;
    }

    public void setApkFiles(String[] files) {
        this.apkFiles = files;
        fileListLiveData.postValue(files);
    }

    public String[] getApkFiles() {
        return apkFiles;
    }

    public void updateFileStatus(int index, String status) {
        if (apkFiles != null && index >= 0 && index < apkFiles.length) {
            apkFiles[index] = status;
            fileListLiveData.postValue(apkFiles);
        }
    }

    public LiveData<String[]> getOutputFileListLiveData() {
        return outputFileListLiveData;
    }

    public void setApkFilesOut(String[] files) {
        this.apkFilesOut = files;
        outputFileListLiveData.postValue(files);
    }

    public String[] getApkFilesOut() {
        return apkFilesOut;
    }

    public synchronized void updateOutputFile(int index, String path) {
        if (apkFilesOut != null && index >= 0 && index < apkFilesOut.length) {
            apkFilesOut[index] = path;
            outputFileListLiveData.postValue(apkFilesOut);
        }
    }
}
