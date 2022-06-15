package com.keepSafe911.fragments.commonfrag


import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.keepSafe911.HomeActivity
import com.keepSafe911.MainActivity


open class HomeDialogBaseFragment : DialogFragment() {

    lateinit var mActivity: HomeActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as HomeActivity
    }
}
