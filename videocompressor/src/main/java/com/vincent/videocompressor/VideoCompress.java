package com.vincent.videocompressor;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Vincent Woo
 * Date: 2017/8/16
 * Time: 15:15
 */

public class VideoCompress {
    private static final String TAG = VideoCompress.class.getSimpleName();

    public static void compressVideoHigh(String srcPath, String destPath, int height, int width, CompressListener listener) {
        videoCompressTask(srcPath, destPath, height, width, listener, VideoController.COMPRESS_QUALITY_HIGH);
    }

    public static void compressVideoMedium(String srcPath, String destPath, int height, int width, CompressListener listener) {
        videoCompressTask(srcPath, destPath, height, width, listener, VideoController.COMPRESS_QUALITY_MEDIUM);
    }

    public static void compressVideoLow(String srcPath, String destPath, int height, int width, CompressListener listener) {
        videoCompressTask(srcPath, destPath, height, width, listener, VideoController.COMPRESS_QUALITY_LOW);
    }

    private static void videoCompressTask(String srcPath, String destPath, int height, int width, CompressListener listener, int quality) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicBoolean isSuccess = new AtomicBoolean(false);
        if (listener != null) {
            listener.onStart();
        }
        executor.execute(() -> {
            if (listener != null) {
                if (destPath != null) {
                    isSuccess.set(VideoController.getInstance().convertVideo(srcPath, destPath, quality, height, width, listener::onProgress));
                }
            }
            handler.post(() -> {
                if (listener != null) {
                    if (isSuccess.get()) {
                        listener.onSuccess();
                    } else {
                        listener.onFail();
                    }
                }
            });
        });
    }

    public interface CompressListener {
        void onStart();
        void onSuccess();
        void onFail();
        void onProgress(float percent);
    }
}
