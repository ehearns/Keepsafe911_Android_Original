package com.keepSafe911.openlive.stats

import java.util.*

class RemoteStatsData : StatsData() {
    var videoDelay = 0
    var audioNetDelay = 0
    var audioNetJitter = 0
    var audioLoss = 0
    var audioQuality: String? = null
    override fun toString(): String {
        return String.format(
            Locale.getDefault(), fORMAT,
            uid,
            width, height, framerate,
            sendQuality, recvQuality,
            videoDelay,
            audioNetDelay, audioNetJitter,
            audioLoss, audioQuality
        )
    }

    companion object {
        const val fORMAT = "Remote(%d)\n\n" +
                "%dx%d %dfps\n" +
                "Quality tx/rx: %s/%s\n" +
                "Video delay: %d ms\n" +
                "Audio net delay/jitter: %dms/%dms\n" +
                "Audio loss/quality: %d%%/%s"
    }
}