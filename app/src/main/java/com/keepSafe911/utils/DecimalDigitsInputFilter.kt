package com.keepSafe911.utils

import android.text.InputFilter
import android.text.Spanned
import android.widget.EditText
import java.util.regex.Pattern

class DecimalDigitsInputFilter(private val digitsBeforeZero: Int, private val digitsAfterZero: Int, private val tempEditText: EditText) : InputFilter {
    private var mPattern: Pattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?")
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence {
        val matcher = mPattern.matcher(dest)
        if (!matcher.matches()) {
            if (dest.toString().contains(".")) {
                val cursorPosition: Int = tempEditText.selectionStart
                val dotPosition: Int = if (dest.toString().indexOf(".") == -1) {
                    dest.toString().indexOf(".")
                } else {
                    dest.toString().indexOf(".")
                }
                if (cursorPosition <= dotPosition) {
                    val beforeDot = dest.toString().substring(0, dotPosition)
                    return if (beforeDot.length < digitsBeforeZero) {
                        source
                    } else {
                        if (source.toString().equals(".", true)) {
                            source
                        } else {
                            ""
                        }
                    }
                } else {
                    if (dest.toString().substring(dest.toString().indexOf(".")).length > digitsAfterZero) {
                        return ""
                    }
                }
            } else if (!Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) + "}").matcher(dest).matches()) {
                if (!dest.toString().contains(".")) {
                    if (source.toString().equals(".", true)) {
                        return source
                    }
                }
                return ""
            } else {
                return source
            }
        }
        return source
    }
}