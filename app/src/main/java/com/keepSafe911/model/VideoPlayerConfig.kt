package com.keepSafe911.model

object VideoPlayerConfig {
    //Minimum Video you want to buffer while Playing
    val MIN_BUFFER_DURATION = 3000
    //Max Video you want to buffer during PlayBack
    val MAX_BUFFER_DURATION = 5000
    //Min Video you want to buffer before start Playing it
    val MIN_PLAYBACK_START_BUFFER = 1500
    //Min video You want to buffer when user resumes video
    val MIN_PLAYBACK_RESUME_BUFFER = 5000
}

