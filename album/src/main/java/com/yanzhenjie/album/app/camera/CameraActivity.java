/*
 * Copyright 2017 Yan Zhenjie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yanzhenjie.album.app.camera;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.R;
import com.yanzhenjie.album.mvp.BaseActivity;
import com.yanzhenjie.album.util.AlbumUtils;
import com.yanzhenjie.album.util.SystemBar;

import java.io.File;

/**
 * Created by YanZhenjie on 2017/8/16.
 */
public class CameraActivity extends BaseActivity {

    private static final String INSTANCE_CAMERA_FUNCTION = "INSTANCE_CAMERA_FUNCTION";
    private static final String INSTANCE_CAMERA_FILE_PATH = "INSTANCE_CAMERA_FILE_PATH";
    private static final String INSTANCE_CAMERA_QUALITY = "INSTANCE_CAMERA_QUALITY";
    private static final String INSTANCE_CAMERA_DURATION = "INSTANCE_CAMERA_DURATION";
    private static final String INSTANCE_CAMERA_BYTES = "INSTANCE_CAMERA_BYTES";

    private static final int CODE_PERMISSION_IMAGE = 1;
    private static final int CODE_PERMISSION_VIDEO = 2;

    private static final int CODE_ACTIVITY_TAKE_IMAGE = 1;
    private static final int CODE_ACTIVITY_TAKE_VIDEO = 2;

    public static Action<String> sResult;
    public static Action<String> sCancel;

    private int mFunction;
    private String mCameraFilePath;
    private int mQuality;
    private long mLimitDuration;
    private long mLimitBytes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBar.setStatusBarColor(this, Color.TRANSPARENT);
        SystemBar.setNavigationBarColor(this, Color.TRANSPARENT);
        SystemBar.invasionNavigationBar(this);
        SystemBar.invasionNavigationBar(this);
        if (savedInstanceState != null) {
            mFunction = savedInstanceState.getInt(INSTANCE_CAMERA_FUNCTION);
            mCameraFilePath = savedInstanceState.getString(INSTANCE_CAMERA_FILE_PATH);
            mQuality = savedInstanceState.getInt(INSTANCE_CAMERA_QUALITY);
            mLimitDuration = savedInstanceState.getLong(INSTANCE_CAMERA_DURATION);
            mLimitBytes = savedInstanceState.getLong(INSTANCE_CAMERA_BYTES);
        } else {
            Bundle bundle = getIntent().getExtras();
            assert bundle != null;
            mFunction = bundle.getInt(Album.KEY_INPUT_FUNCTION);
            mCameraFilePath = bundle.getString(Album.KEY_INPUT_FILE_PATH);
            mQuality = bundle.getInt(Album.KEY_INPUT_CAMERA_QUALITY);
            mLimitDuration = bundle.getLong(Album.KEY_INPUT_CAMERA_DURATION);
            mLimitBytes = bundle.getLong(Album.KEY_INPUT_CAMERA_BYTES);

            switch (mFunction) {
                case Album.FUNCTION_CAMERA_IMAGE: {
                    if (TextUtils.isEmpty(mCameraFilePath))
                        mCameraFilePath = AlbumUtils.randomJPGPath(this);
                    requestPermission(PERMISSION_TAKE_PICTURE, CODE_PERMISSION_IMAGE);
                    break;
                }
                case Album.FUNCTION_CAMERA_VIDEO: {
                    if (TextUtils.isEmpty(mCameraFilePath))
                        mCameraFilePath = AlbumUtils.randomMP4Path(this);
                    requestPermission(PERMISSION_TAKE_VIDEO, CODE_PERMISSION_VIDEO);
                    break;
                }
                default: {
                    throw new AssertionError("This should not be the case.");
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(INSTANCE_CAMERA_FUNCTION, mFunction);
        outState.putString(INSTANCE_CAMERA_FILE_PATH, mCameraFilePath);
        outState.putInt(INSTANCE_CAMERA_QUALITY, mQuality);
        outState.putLong(INSTANCE_CAMERA_DURATION, mLimitDuration);
        outState.putLong(INSTANCE_CAMERA_BYTES, mLimitBytes);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPermissionGranted(int code) {
        switch (code) {
            case CODE_PERMISSION_IMAGE: {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri uri = AlbumUtils.getUri(this, new File(mCameraFilePath));
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraActivityResultLauncher.launch(intent);
                break;
            }
            case CODE_PERMISSION_VIDEO: {
                AlbumUtils.takeVideo(this, CODE_ACTIVITY_TAKE_VIDEO, new File(mCameraFilePath), mQuality, mLimitDuration, mLimitBytes);
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                Uri uri = AlbumUtils.getUri(this, new File(mCameraFilePath));
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, mQuality);
                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, mLimitDuration);
                intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, mLimitBytes);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraActivityResultLauncher.launch(intent);
                break;
            }
            default: {
                throw new AssertionError("This should not be the case.");
            }
        }
    }

    ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        callbackResult();
                    } else {
                        callbackCancel();
                    }
                }
            }
    );

    @Override
    protected void onPermissionDenied(int code) {
        int messageRes;
        switch (mFunction) {
            case Album.FUNCTION_CAMERA_IMAGE: {
                messageRes = R.string.album_permission_camera_image_failed_hint;
                break;
            }
            case Album.FUNCTION_CAMERA_VIDEO: {
                messageRes = R.string.album_permission_camera_video_failed_hint;
                break;
            }
            default: {
                throw new AssertionError("This should not be the case.");
            }
        }
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.album_title_permission_failed)
                .setMessage(messageRes)
                .setPositiveButton(R.string.album_ok, (dialog, which) -> callbackCancel())
                .show();
    }

    private void callbackResult() {
        if (sResult != null) sResult.onAction(mCameraFilePath);
        sResult = null;
        sCancel = null;
        finish();
    }

    private void callbackCancel() {
        if (sCancel != null) sCancel.onAction("User canceled.");
        sResult = null;
        sCancel = null;
        finish();
    }
}