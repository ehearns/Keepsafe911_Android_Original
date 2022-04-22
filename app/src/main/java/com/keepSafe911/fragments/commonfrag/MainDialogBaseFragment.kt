package com.keepSafe911.fragments.commonfrag


import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.keepSafe911.MainActivity


open class MainDialogBaseFragment : DialogFragment() {

    lateinit var mActivity: MainActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as MainActivity
    }
}
