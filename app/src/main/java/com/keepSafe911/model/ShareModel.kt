package com.keepSafe911.model

import com.keepSafe911.R

class ShareModel(var shareLogo: Int, var shareText: Int) {

    companion object{
        val getShareLinks: ArrayList<ShareModel>
            get() {
                val mapFilter =  ArrayList<ShareModel>()
                mapFilter.add(ShareModel(R.drawable.ic_share_insta, R.string.str_instagram))
                mapFilter.add(ShareModel(R.drawable.ic_share_twitter, R.string.str_twitter))
                mapFilter.add(ShareModel(R.drawable.ic_share_fb, R.string.str_facebook))
                mapFilter.add(ShareModel(R.drawable.ic_share_gmail, R.string.str_gmail))
                mapFilter.add(ShareModel(R.drawable.ic_share_linked, R.string.str_linked_in))
                mapFilter.add(ShareModel(R.drawable.ic_share_whatsapp, R.string.str_whatsapp))
                return mapFilter
            }
    }
}