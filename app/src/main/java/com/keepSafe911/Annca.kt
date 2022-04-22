package com.keepSafe911

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.annotation.IntRange
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import com.keepSafe911.internal.configuration.AnncaConfiguration
import com.keepSafe911.internal.ui.camera.Camera1Activity
import com.keepSafe911.internal.ui.camera2.Camera2Activity
import com.keepSafe911.internal.utils.CameraHelper

/**
 * Created by memfis on 7/6/16.
 */
class Annca {
    private var anncaConfiguration: AnncaConfiguration?

    /***
     * Creates Annca instance with default configuration set to photo with medium quality.
     *
     * @param activity    - fromList which request was invoked
     * @param requestCode - request code which will return in onActivityForResult
     */
    constructor(activity: Activity?, @IntRange(from = 0) requestCode: Int) {
        val builder = AnncaConfiguration.Builder(activity!!, requestCode)
        anncaConfiguration = builder.build()
    }

    constructor(fragment: Fragment?, @IntRange(from = 0) requestCode: Int) {
        val builder: AnncaConfiguration.Builder = AnncaConfiguration.Builder(fragment!!, requestCode)
        anncaConfiguration = builder.build()
    }

    /***
     * Creates Annca instance with custom camera configuration.
     *
     * @param cameraConfiguration
     */
    constructor(cameraConfiguration: AnncaConfiguration?) {
        anncaConfiguration = cameraConfiguration
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun launchCamera() {
        if (anncaConfiguration == null || anncaConfiguration!!.activity == null && anncaConfiguration!!.fragment == null) return
        val cameraIntent: Intent
        if (CameraHelper.hasCamera2(anncaConfiguration!!.activity)) {
            cameraIntent = if (anncaConfiguration!!.fragment != null) Intent(
                anncaConfiguration!!.fragment.context,
                Camera2Activity::class.java
            ) else Intent(
                anncaConfiguration!!.activity,
                Camera2Activity::class.java
            )
        } else {
            cameraIntent = if (anncaConfiguration!!.fragment != null) Intent(
                anncaConfiguration!!.fragment.context,
                Camera1Activity::class.java
            ) else Intent(
                anncaConfiguration!!.activity,
                Camera1Activity::class.java
            )
        }
        cameraIntent.putExtra(
            AnncaConfiguration.Arguments.REQUEST_CODE,
            anncaConfiguration?.requestCode
        )
        cameraIntent.putExtra(
            AnncaConfiguration.Arguments.CAMERA_FACE,
            anncaConfiguration?.cameraFace
        )
        cameraIntent.putExtra(
            AnncaConfiguration.Arguments.FILE_PATH,
            anncaConfiguration?.outPutFilePath ?: ""
        )
        cameraIntent.putExtra(
            AnncaConfiguration.Arguments.MEDIA_RESULT_BEHAVIOUR,
            anncaConfiguration?.mediaResultBehaviour
        )
        if ((anncaConfiguration?.mediaAction ?: 0) > 0) cameraIntent.putExtra(
            AnncaConfiguration.Arguments.MEDIA_ACTION,
            anncaConfiguration?.mediaAction
        )
        if ((anncaConfiguration?.mediaQuality ?: 0) > 0) cameraIntent.putExtra(
            AnncaConfiguration.Arguments.MEDIA_QUALITY,
            anncaConfiguration?.mediaQuality
        )
        if ((anncaConfiguration?.videoDuration ?: 0) > 0) cameraIntent.putExtra(
            AnncaConfiguration.Arguments.VIDEO_DURATION,
            anncaConfiguration?.videoDuration
        )
        if ((anncaConfiguration?.videoFileSize ?: 0) > 0) cameraIntent.putExtra(
            AnncaConfiguration.Arguments.VIDEO_FILE_SIZE,
            anncaConfiguration?.videoFileSize
        )
        if ((anncaConfiguration?.minimumVideoDuration ?: 0) > 0) cameraIntent.putExtra(
            AnncaConfiguration.Arguments.MINIMUM_VIDEO_DURATION,
            anncaConfiguration?.minimumVideoDuration
        )
        cameraIntent.putExtra(
            AnncaConfiguration.Arguments.FLASH_MODE,
            anncaConfiguration?.flashMode
        )
        cameraIntent.putExtra(
            AnncaConfiguration.Arguments.VIDEO_AUTO_START,
            anncaConfiguration?.autoStartVideo ?: false
        )
        cameraIntent.putExtra(
            AnncaConfiguration.Arguments.SEND_MAIL,
            anncaConfiguration?.sendMail ?: false
        )
        if (anncaConfiguration!!.fragment != null) {
            anncaConfiguration!!.fragment
                .startActivityForResult(cameraIntent, anncaConfiguration!!.requestCode)
        } else {
            anncaConfiguration!!.activity
                .startActivityForResult(cameraIntent, anncaConfiguration!!.requestCode)
        }
    }
}