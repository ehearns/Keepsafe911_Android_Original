package com.keepSafe911.fragments.commonfrag


import android.os.Bundle
import androidx.fragment.app.Fragment
import com.keepSafe911.HomeActivity


open class HomeBaseFragment : Fragment() {

    lateinit var mActivity: HomeActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as HomeActivity
    }
}
