package com.keepSafe911.model

import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.R
import com.keepSafe911.fragments.neighbour.NeighbourFragment
import com.keepSafe911.model.response.FeedResponseResult
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

import java.util.ArrayList

class ExoPlayerRecyclerView : RecyclerView {

    private var videoInfoList: List<FeedResponseResult> = ArrayList()
    private var videoSurfaceDefaultHeight = 0
    private var screenDefaultHeight = 0
    private var player: SimpleExoPlayer? = null
    //surface view for playing video
    private var videoSurfaceView: PlayerView? = null
    private var mProgressBar: ProgressBar? = null
    private var appContext: Context? = null
    private var mPlayVideo: ImageView? = null


    /**
     * the position of playing video
     */
    private var playPosition = -1

    private var addedVideo = false
    private var rowParent: View? = null

    /**
     * {@inheritDoc}
     *
     * @param context
     */
    constructor(context: Context) : super(context) {
        initialize(context)
    }

    /**
     * {@inheritDoc}
     *
     * @param context
     * @param attrs
     */
    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs) {
        initialize(context)
    }

    /**
     * {@inheritDoc}
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initialize(context)
    }

    fun setVideoInfoList(videoInfoList: List<FeedResponseResult>) {
        this.videoInfoList = videoInfoList

    }


    /**
     * prepare for video play
     */
    //remove the player from the row
    private fun removeVideoView(videoView: PlayerView) {

        videoView.isSoundEffectsEnabled = false
        val parent = if (videoView.parent!=null) videoView.parent as ViewGroup else return
        val index = parent.indexOfChild(videoView)
        if (index >= 0) {
            parent.removeViewAt(index)
            addedVideo = false
        }

    }

    //play the video in the row
    fun playVideo() {
        val startPosition = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        var endPosition = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

        if (endPosition - startPosition > 1) {
            endPosition = startPosition + 1
        }

        if (startPosition < 0 || endPosition < 0) {
            return
        }

        val targetPosition: Int
        targetPosition = if (startPosition != endPosition) {
            val startPositionVideoHeight = getVisibleVideoSurfaceHeight(startPosition)
            val endPositionVideoHeight = getVisibleVideoSurfaceHeight(endPosition)
            if (startPositionVideoHeight > endPositionVideoHeight) startPosition else endPosition
        } else {
            startPosition
        }

        if (targetPosition < 0 || targetPosition == playPosition) {
            return
        }
        playPosition = targetPosition
        if (videoSurfaceView == null) {
            return
        }
        videoSurfaceView?.visibility = View.INVISIBLE
        removeVideoView(videoSurfaceView!!)

        // get target View targetPosition in RecyclerView
        val at = targetPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        val child = getChildAt(at) ?: return

        val holder: NeighbourFragment.NeighbourAdapter.NeighbourHolder
        if (child.tag == null) {
            playPosition = -1
            return
        } else {
            holder = child.tag as NeighbourFragment.NeighbourAdapter.NeighbourHolder
        }
        mPlayVideo = holder.ivPlayVideo
        if (mPlayVideo!=null){
            mPlayVideo?.visibility = View.GONE
        }
        mProgressBar = holder.progressBar
        val frameLayout = holder.itemView.findViewById<FrameLayout>(R.id.flPostFileVideo)
        frameLayout.addView(videoSurfaceView)
        videoSurfaceView?.isSoundEffectsEnabled = true
        addedVideo = true
        rowParent = holder.itemView
        videoSurfaceView?.requestFocus()
        // Bind the player to the view.
        videoSurfaceView?.player = player

        // Measures bandwidth during playback. Can be null if not required.
        val defaultBandwidthMeter = DefaultBandwidthMeter()
        // Produces DataSource instances through which media data is loaded.

        val dataSourceFactory = DefaultDataSourceFactory(
            appContext!!,
            Util.getUserAgent(appContext, "android_wave_list"), defaultBandwidthMeter
        )
        // This is the MediaSource representing the media to be played.
        val uriString = if (videoInfoList[targetPosition].file!=null)videoInfoList[targetPosition].file else ""
        if (uriString != null) {
            if (uriString != "") {
                val videoSource = ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(uriString))
                // Prepare the player with the source.
                if (player != null) {
                    if (videoSource != null) {
                        player?.prepare(videoSource)
                        player?.playWhenReady = true
                    } else {
                        player?.playWhenReady = false
                    }
                }
            }
        }
        if (videoInfoList[targetPosition].fileType != 1) {
            if (videoSurfaceView == null) {
                return
            }
            videoSurfaceView?.visibility = View.INVISIBLE
            if (videoSurfaceView!=null) {
                videoSurfaceView?.isSoundEffectsEnabled = false
                removeVideoView(videoSurfaceView!!)
            }
        }
    }

    private fun getVisibleVideoSurfaceHeight(playPosition: Int): Int {
        val at = playPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        val child = getChildAt(at) ?: return 0

        val location01 = IntArray(2)
        child.getLocationInWindow(location01)

        return if (location01[1] < 0) {
            location01[1] + videoSurfaceDefaultHeight
        } else {
            screenDefaultHeight - location01[1]
        }
    }


    private fun initialize(context: Context) {

        appContext = context.applicationContext
        val display = (getContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val point = Point()
        display.getSize(point)
        videoSurfaceDefaultHeight = point.x

        screenDefaultHeight = point.y
        videoSurfaceView = PlayerView(appContext)
//        videoSurfaceView?.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM)

        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val loadControl = DefaultLoadControl(
            DefaultAllocator(true, 16),
            VideoPlayerConfig.MIN_BUFFER_DURATION,
            VideoPlayerConfig.MAX_BUFFER_DURATION,
            VideoPlayerConfig.MIN_PLAYBACK_START_BUFFER,
            VideoPlayerConfig.MIN_PLAYBACK_RESUME_BUFFER, -1, true
        )

        // 2. Create the player
        player = ExoPlayerFactory.newSimpleInstance(appContext, trackSelector, loadControl)
        // Bind the player to the view.
        videoSurfaceView?.useController = false
        videoSurfaceView?.player = player

        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
//                    playVideo()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (mPlayVideo!=null){
                    if (mPlayVideo?.visibility == View.GONE) {
                        mPlayVideo?.visibility = View.VISIBLE
                        if (videoSurfaceView != null) {
                            onPausePlayer()
                        }
                    }
                }
            }
        })

        addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {

            }

            override fun onChildViewDetachedFromWindow(view: View) {
                if (addedVideo && rowParent != null && rowParent == view) {
                    videoSurfaceView?.isSoundEffectsEnabled = false
                    removeVideoView(videoSurfaceView!!)
                    playPosition = -1
                    videoSurfaceView?.visibility = View.INVISIBLE
                }
            }
        })
        player?.addListener(object : Player.EventListener {
            override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {}

            override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

            override fun onLoadingChanged(isLoading: Boolean) {}

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {

                    Player.STATE_BUFFERING -> {
                        videoSurfaceView?.alpha = 0.5f
                        Log.e(TAG, "onPlayerStateChanged: Buffering ")
                        if (mProgressBar != null) {
                            mProgressBar?.visibility = View.VISIBLE
                        }
                    }
                    Player.STATE_ENDED -> {
                        player?.seekTo(0)
                        onRestartPlayer()
                    }
                    Player.STATE_IDLE -> {
                    }
                    Player.STATE_READY -> {
                        Log.e(TAG, "onPlayerStateChanged: Ready ")
                        if (mProgressBar != null) {
                            mProgressBar?.visibility = View.GONE
                        }
                        videoSurfaceView?.visibility = View.VISIBLE
                        videoSurfaceView?.alpha = 1f
                    }
                    else -> {
                    }
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {

            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

            }

            override fun onPlayerError(error: ExoPlaybackException) {

            }

            override fun onPositionDiscontinuity(reason: Int) {

            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {

            }

            override fun onSeekProcessed() {

            }
        })
    }

    fun onPausePlayer() {
        if (videoSurfaceView != null) {
            if (mProgressBar!=null) {
                mProgressBar?.visibility = View.GONE
            }
            if (videoSurfaceView!=null) {
                videoSurfaceView?.isSoundEffectsEnabled = false
                removeVideoView(videoSurfaceView!!)
            }
            player?.release()
            videoSurfaceView = null
        }
    }

    fun onRestartPlayer() {
        if (videoSurfaceView == null) {
            if (mPlayVideo!=null){
                mPlayVideo?.visibility = View.GONE
            }
            playPosition = -1
            if (appContext!=null){
                initialize(appContext!!)
            }
            playVideo()
        }
    }

    /**
     * release memory
     */
    fun onRelease() {

        if (player != null) {
            if (mProgressBar!=null) {
                mProgressBar?.visibility = View.GONE
            }
            if (videoSurfaceView!=null) {
                videoSurfaceView?.isSoundEffectsEnabled = false
                removeVideoView(videoSurfaceView!!)
            }
            player?.release()
            videoSurfaceView = null
            player = null
        }

        rowParent = null
    }

    companion object {
        private const val TAG = "ExoPlayerRecyclerView"
    }


}

