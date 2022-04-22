package com.keepSafe911.utils

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.keepSafe911.R
import com.keepSafe911.listner.PositiveButtonListener


open class CustomPopUpDialog : Dialog {

    constructor(context: Context) : super(context) {}

    constructor(context: Context, theme: Int) : super(context, theme) {}

    companion object {

        private var dialog: CustomPopUpDialog? = null

        fun show(
            context: Context, title: String, message: String, positiveButtonText: String,
            negativeButtonText: String, positiveButtonListener: PositiveButtonListener?,
            singlePositive: Boolean, singleNegative: Boolean
        ): CustomPopUpDialog? {

            try {
                dialog = CustomPopUpDialog(context, R.style.PaymentNewDialog)
                dialog?.setTitle("")
                dialog?.setContentView(R.layout.popup_custom_dialog)

                val llCustomDialog: LinearLayout? = dialog?.findViewById(R.id.llCustomDialog)
                val tvPopUpTitle: TextView? = dialog?.findViewById(R.id.tvPopUpTitle)
                val tvPopUpMessage: TextView? = dialog?.findViewById(R.id.tvPopUpMessage)
                val tvCancelPopUp: TextView? = dialog?.findViewById(R.id.tvCancelPopUp)
                val tvApprovePopUp: TextView? = dialog?.findViewById(R.id.tvApprovePopUp)
                val popupDivider: View? = dialog?.findViewById(R.id.popupDivider)

                val weight = if (context.resources.getBoolean(R.bool.isTablet)) {
                    Utils.calculateNoOfColumns(context, 2.4)
                } else {
                    Utils.calculateNoOfColumns(context, 1.2)
                }

                val layoutParams = llCustomDialog?.layoutParams
                layoutParams?.width = weight
                llCustomDialog?.layoutParams = layoutParams

                tvPopUpTitle?.text = title
                tvPopUpMessage?.text = message
                tvApprovePopUp?.text = positiveButtonText
                tvCancelPopUp?.text = negativeButtonText

                when {
                    singlePositive -> {
                        popupDivider?.visibility = View.GONE
                        tvCancelPopUp?.visibility = View.GONE
                    }
                    singleNegative -> {
                        popupDivider?.visibility = View.GONE
                        tvApprovePopUp?.visibility = View.GONE
                    }
                    else -> {
                        popupDivider?.visibility = View.VISIBLE
                        tvCancelPopUp?.visibility = View.VISIBLE
                        tvApprovePopUp?.visibility = View.VISIBLE
                    }
                }

                tvApprovePopUp?.setOnClickListener {
                    positiveButtonListener?.okClickListener()
                    dialogDismiss()
                }

                tvCancelPopUp?.setOnClickListener {
                    positiveButtonListener?.cancelClickLister()
                    dialogDismiss()
                }

                dialog?.setCancelable(false)
                dialog?.window?.attributes?.gravity = Gravity.CENTER

                val lp = dialog?.window?.attributes
                lp?.dimAmount = 0.3f
                dialog?.window?.attributes = lp
                dialog?.show()
                return dialog as CustomPopUpDialog
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
