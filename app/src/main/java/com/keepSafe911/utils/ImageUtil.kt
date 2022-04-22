package com.keepSafe911.utils

import android.content.Context
import android.net.Uri
import android.util.DisplayMetrics
import android.view.WindowManager
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.backends.pipeline.PipelineDraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.keepSafe911.R
import java.io.File

/**
 *  This method is used  to get max width as per display.
 */
fun getMaxWidth(context: Context): Int {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val metrics = DisplayMetrics()
    display.getMetrics(metrics)
    return metrics.widthPixels
}

private var request: ImageRequest? = null

/**
 *  This method  is used load  fresco image from url.
 */
fun SimpleDraweeView.loadFrescoImage(context: Context, imagePath: String, aspectRatio: Int) {
    var viewAspectRatio: Int = 1
    if (aspectRatio == 0) {
        viewAspectRatio = 1
    }

    val uri = Uri.parse(imagePath)
    val imageSize = getMaxWidth(context) / viewAspectRatio

    request = ImageRequestBuilder.newBuilderWithSource(uri)
            .setResizeOptions(ResizeOptions(imageSize, imageSize))
            .build()
    controller = Fresco.newDraweeControllerBuilder()
            .setOldController(this@loadFrescoImage.controller)
            .setImageRequest(request)

            .build() as PipelineDraweeController

    this@loadFrescoImage.controller = controller
}

/**
 *  This method  is used load  fresco image from url.
 */
fun SimpleDraweeView.loadImage(context: Context, imagePath: String) {
    this.loadFrescoImage(context, imagePath, 1)
}

/**
 *  This method  is used load  fresco image from file.
 */
fun SimpleDraweeView.loadFrescoImageFromFile(context: Context, file: File?, aspectRatio: Int = 1) {

    try {
        if (file != null && file.exists()) {
            val uri = Uri.fromFile(file)
            val imageSize = getMaxWidth(context) / aspectRatio

            request = ImageRequestBuilder.newBuilderWithSource(uri)
                    .setResizeOptions(ResizeOptions(imageSize, imageSize))
                    .build()
            controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(this.controller)
                    .setImageRequest(request)
                    .build() as PipelineDraweeController
            this.controller = controller
        } else {
            request = ImageRequestBuilder.newBuilderWithResourceId(R.drawable.upload_profile_green)
                    .build()


            controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(this.controller)
                    .setImageRequest(request)
                    .build() as PipelineDraweeController
            this.controller = controller
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 *  This method  is used load  fresco image from resource.
 */
fun SimpleDraweeView.loadFrescoImageFromResource(context: Context, resourceId: Int, aspectRatio: Int) {
    var viewAspectRatio: Int = 1
    if (aspectRatio == 0) {
        viewAspectRatio = 1
    }
    try {
        if (resourceId != 0) {
            val imageSize = getMaxWidth(context) / viewAspectRatio

            request = ImageRequestBuilder.newBuilderWithResourceId(resourceId)
                    //                        .setResizeOptions(new ResizeOptions(imageSize, imageSize))
                    .build()
            controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(this.controller)
                    .setImageRequest(request)
                    .build() as PipelineDraweeController
            this.controller = controller
        } else run {

            request = ImageRequestBuilder.newBuilderWithResourceId(R.drawable.upload_profile_green)
                    .build()


            controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(this.controller)
                    .setImageRequest(request)
                    .build() as PipelineDraweeController
            this.controller = controller
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}