package com.keepSafe911.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.keepSafe911.utils.Utils


class CountryAdapter(
    context: Context, @LayoutRes private val layoutResource: Int,
    private val country_code_list: List<PhoneCountryCode>
) :
    ArrayAdapter<PhoneCountryCode>(context, layoutResource, country_code_list) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        return createViewFromResource(position, convertView, parent)

    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var textView: TextView? = super.getView(position, convertView, parent!!) as TextView

        if (textView == null) {
            textView = TextView(context)
        }
        textView.text = country_code_list[position].countryCode + " " + country_code_list[position].countryName
        return textView
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup?): View {
        val textView: TextView =
            convertView as TextView? ?: LayoutInflater.from(context).inflate(layoutResource, parent, false) as TextView
        textView.text = country_code_list[position].countryCode
        textView.setCompoundDrawablesWithIntrinsicBounds(Utils.countryFlagWithCode(context)[position].flag, 0, 0, 0)
        println("!@@position = ${position} ${country_code_list[position].countryCode}")

        return textView
    }
}