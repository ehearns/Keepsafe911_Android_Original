package com.keepSafe911.openlive.rtc

import com.keepSafe911.openlive.Constants


class EngineConfig {
    // private static final int DEFAULT_UID = 0;
    // private int mUid = DEFAULT_UID;
    var channelName: String? = null
    var mShowVideoStats = false
    var videoDimenIndex: Int = Constants.DEFAULT_PROFILE_IDX
    var mirrorLocalIndex = 0
    var mirrorRemoteIndex = 0
    var mirrorEncodeIndex = 0
    var userRole = 0
}