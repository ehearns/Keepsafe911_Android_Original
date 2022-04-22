package com.keepSafe911.openlive.rtc

import io.agora.rtc.IRtcEngineEventHandler

interface EventHandler {
    fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int)
    fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats?)
    fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int)
    fun onUserOffline(uid: Int, reason: Int)
    fun onUserJoined(uid: Int, elapsed: Int)
    fun onLastmileQuality(quality: Int)
    fun onLastmileProbeResult(result: IRtcEngineEventHandler.LastmileProbeResult?)
    fun onLocalVideoStats(stats: IRtcEngineEventHandler.LocalVideoStats?)
    fun onRtcStats(stats: IRtcEngineEventHandler.RtcStats?)
    fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int)
    fun onRemoteVideoStats(stats: IRtcEngineEventHandler.RemoteVideoStats?)
    fun onRemoteAudioStats(stats: IRtcEngineEventHandler.RemoteAudioStats?)
}