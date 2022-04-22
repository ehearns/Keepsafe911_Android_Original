package com.keepSafe911.utils

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keepSafe911.R


open class PaymentDialog : Dialog {

    constructor(context: Context) : super(context) {}

    constructor(context: Context, theme: Int) : super(context, theme) {}

    companion object {

        private var dialog: PaymentDialog? = null

        fun show(
            context: Context, paymentType: Int = 1
        ): PaymentDialog? {

            try {
                dialog = PaymentDialog(context, R.style.PaymentNewDialog)
                dialog?.setTitle("")
                dialog?.setContentView(R.layout.popup_payment_fail_success)

                val llPaymentPopUp: LinearLayout? = dialog?.findViewById(R.id.llPaymentPopUp)
                val tvPaymentStatus: TextView? = dialog?.findViewById(R.id.tvPaymentStatus)
                val tvPaymentMessage: TextView? = dialog?.findViewById(R.id.tvPaymentMessage)
                val tvTryAgain: TextView? = dialog?.findViewById(R.id.tvTryAgain)
                val ivPaymentStatus: ImageView? = dialog?.findViewById(R.id.ivPaymentStatus)
                tvTryAgain?.visibility = View.INVISIBLE

                val weight = if (context.resources.getBoolean(R.bool.isTablet)) {
                    Utils.calculateNoOfColumns(context, 3.0)
                } else {
                    Utils.calculateNoOfColumns(context, 1.5)
                }

                val layoutParams = llPaymentPopUp?.layoutParams
                layoutParams?.width = weight
                llPaymentPopUp?.layoutParams = layoutParams

                if (paymentType > 0) {
                    llPaymentPopUp?.background = ContextCompat.getDrawable(context, R.drawable.payment_failure)
                    ivPaymentStatus?.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_payment_fail))
                    tvPaymentStatus?.text = context.resources.getString(R.string.str_payment_fail)
                    tvPaymentMessage?.text = context.resources.getString(R.string.transaction_declined)
                    tvTryAgain?.visibility = View.VISIBLE
                } else {
                    llPaymentPopUp?.background = ContextCompat.getDrawable(context, R.drawable.payment_success)
                    ivPaymentStatus?.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_payment_success))
                    tvPaymentStatus?.text = context.resources.getString(R.string.str_payment_success)
                    tvPaymentMessage?.text = context.resources.getString(R.string.str_payment_success_msg)
                }
                tvTryAgain?.setOnClickListener {
                    dialog?.dismiss()
                }

                dialog?.setCancelable(false)
                dialog?.window?.attributes?.gravity = Gravity.CENTER

                val lp = dialog?.window?.attributes
                lp?.dimAmount = 0.2f
                dialog?.window?.attributes = lp
                dialog?.show()
                return dialog as PaymentDialog
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
