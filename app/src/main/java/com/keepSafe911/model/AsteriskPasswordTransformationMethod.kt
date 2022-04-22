package com.keepSafe911.model

import android.text.method.PasswordTransformationMethod
import android.view.View

class AsteriskPasswordTransformationMethod : PasswordTransformationMethod() {
    override fun getTransformation(source: CharSequence, view: View): CharSequence {
        return PasswordCharSequence(source)
    }

    private inner class PasswordCharSequence(private val mSource: CharSequence)// Store char sequence
        : CharSequence {
        override val length: Int
            get() = mSource.length

        override fun get(index: Int): Char {
            return if (index == 4 || index == 9 || index >= 14) {
                mSource[index]
            } else {
                '\u25CF'
            }
        }

        override fun subSequence(start: Int, end: Int): CharSequence {
            return mSource.subSequence(start, end) // Return default
        }
    }
}