package com.keepSafe911.openlive

import io.agora.rtc.video.VideoEncoderConfiguration

object Constants {

    var VIDEO_DIMENSIONS: Array<VideoEncoderConfiguration.VideoDimensions> =
        arrayOf(
            VideoEncoderConfiguration.VD_320x240,
            VideoEncoderConfiguration.VD_480x360,
            VideoEncoderConfiguration.VD_640x360,
            VideoEncoderConfiguration.VD_640x480,
            VideoEncoderConfiguration.VideoDimensions(960, 540),
            VideoEncoderConfiguration.VD_1280x720
        )
    var VIDEO_MIRROR_MODES = intArrayOf(
        io.agora.rtc.Constants.VIDEO_MIRROR_MODE_AUTO,
        io.agora.rtc.Constants.VIDEO_MIRROR_MODE_ENABLED,
        io.agora.rtc.Constants.VIDEO_MIRROR_MODE_DISABLED
    )
    const val PREF_NAME = "io.agora.openlive"
    const val DEFAULT_PROFILE_IDX = 2
    const val PREF_RESOLUTION_IDX = "pref_profile_index"
    const val PREF_ENABLE_STATS = "pref_enable_stats"
    const val PREF_MIRROR_LOCAL = "pref_mirror_local"
    const val PREF_MIRROR_REMOTE = "pref_mirror_remote"
    const val PREF_MIRROR_ENCODE = "pref_mirror_encode"
    const val KEY_CLIENT_ROLE = "key_client_role"
    const val KEY_CHANNEL_NAME = "key_channel_name"
    const val KEY_CHANNEL_ID = "key_channel_id"
    const val MISSING_CHILD_ID = "key_missing_child_id"
}