package com.keepSafe911.openlive.stats

import java.util.*

class LocalStatsData : StatsData() {
    var lastMileDelay = 0
    var videoSendBitrate = 0
    var videoRecvBitrate = 0
    var audioSendBitrate = 0
    var audioRecvBitrate = 0
    var cpuApp = 0.0
    var cpuTotal = 0.0
    var sendLoss = 0
    var recvLoss = 0
    override fun toString(): String {
        return String.format(
            Locale.getDefault(), FORMAT,
            uid,
            width, height, framerate,
            lastMileDelay,
            videoSendBitrate, videoRecvBitrate,
            audioSendBitrate, audioRecvBitrate,
            cpuApp, cpuTotal,
            sendQuality, recvQuality,
            sendLoss, recvLoss
        )
    }

    companion object {
        private const val FORMAT = "Local(%d)\n\n" +
                "%dx%d %dfps\n" +
                "LastMile delay: %d ms\n" +
                "Video tx/rx (kbps): %d/%d\n" +
                "Audio tx/rx (kbps): %d/%d\n" +
                "CPU: app/total %.1f%%/%.1f%%\n" +
                "Quality tx/rx: %s/%s\n" +
                "Loss tx/rx: %d%%/%d%%"
    }
}