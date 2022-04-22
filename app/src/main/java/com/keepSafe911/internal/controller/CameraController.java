package com.keepSafe911.internal.controller;

import android.os.Bundle;

import com.keepSafe911.internal.configuration.AnncaConfiguration;
import com.keepSafe911.internal.manager.CameraManager;

import java.io.File;

/**
 * Created by memfis on 7/6/16.
 */
public interface CameraController<CameraId> {

    void onCreate(Bundle savedInstanceState);

    void onResume();

    void onPause();

    void onDestroy();

    void takePhoto();

    void startVideoRecord();

    void stopVideoRecord();

    boolean isVideoRecording();

    void openCamera();

    void switchCamera(@AnncaConfiguration.CameraFace int cameraFace);

    void switchQuality();

    void setFlashMode(@AnncaConfiguration.FlashMode int flashMode);

    int getNumberOfCameras();

    @AnncaConfiguration.MediaAction
    int getMediaAction();

    CameraId getCurrentCameraId();

    File getOutputFile();

    CameraManager getCameraManager();
}
