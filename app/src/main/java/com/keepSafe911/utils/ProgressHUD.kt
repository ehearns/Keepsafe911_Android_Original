package com.keepSafe911.utils

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.view.Gravity
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.keepSafe911.R


open class ProgressHUD : Dialog {


    constructor(context: Context) : super(context) {}

    constructor(context: Context, theme: Int) : super(context, theme) {}

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        val imageView = findViewById<ImageView>(R.id.spinnerImageView)
        val spinner = imageView
            .background as AnimationDrawable
        spinner.start()
    }

    companion object {

        private var dialog: ProgressHUD? = null

        fun show(
            context: Context, message: CharSequence? = "",
            indeterminate: Boolean = false, cancelable: Boolean = false
        ): ProgressHUD? {

            try {
                dialog = ProgressHUD(context, R.style.NewDialog)
                dialog?.setTitle("")
                dialog?.setContentView(R.layout.progress_hud)

                val currentApiVersion = Build.VERSION.SDK_INT
                if (currentApiVersion < Build.VERSION_CODES.LOLLIPOP) {
                    dialog?.findViewById<ProgressBar>(R.id.pb_load)?.background =
                        ContextCompat.getDrawable(context,R.drawable.custom_progress_bar)
                }

                dialog?.setCancelable(cancelable)
//            dialog?.setOnCancelListener(cancelListener)
                dialog?.window?.attributes?.gravity = Gravity.CENTER

                val lp = dialog?.window?.attributes
                lp?.dimAmount = 0.2f
                dialog?.window?.attributes = lp
                // dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                dialog?.show()
                return dialog as ProgressHUD
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        fun dialogDismiss() {
            dialog?.dismiss()
        }
    }

}
