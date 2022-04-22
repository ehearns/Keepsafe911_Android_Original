package com.keepSafe911.openlive.ui

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.util.AttributeSet
import android.util.SparseArray
import android.view.SurfaceView
import android.view.TextureView
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.keepSafe911.openlive.stats.StatsData
import com.keepSafe911.openlive.stats.StatsManager
import com.keepSafe911.R
import java.util.*

class VideoGridContainer : RelativeLayout, Runnable {
    private val mUserViewList: SparseArray<ViewGroup> = SparseArray<ViewGroup>(MAX_USER)
    private val mUidList: MutableList<Int> = ArrayList(MAX_USER)
    private var mStatsManager: StatsManager? = null
    private var mHandler: Handler? = null
    private var mStatMarginBottom = 0

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        setBackgroundResource(R.drawable.live_room_bg)
        mStatMarginBottom = resources.getDimensionPixelSize(
            R.dimen.live_stat_margin_bottom
        )
        mHandler = Handler(context.mainLooper)
    }

    fun setStatsManager(manager: StatsManager?) {
        mStatsManager = manager
    }

    fun addUserVideoSurface(uid: Int, surface: TextureView?, isLocal: Boolean) {
        if (surface == null) {
            return
        }
        var id = -1
        if (isLocal) {
            if (mUidList.contains(0)) {
                mUidList.remove(0)
                mUserViewList.remove(0)
            }
            if (mUidList.size == MAX_USER) {
                mUidList.removeAt(0)
                mUserViewList.remove(0)
            }
            id = 0
        } else {
            if (mUidList.contains(uid)) {
                mUidList.remove(uid)
                mUserViewList.remove(uid)
            }
            if (mUidList.size < MAX_USER) {
                id = uid
            }
        }
        if (id == 0) mUidList.add(0, uid) else mUidList.add(uid)
        if (id != -1) {
            mUserViewList.append(uid, createVideoView(surface))
            if (mStatsManager != null) {
                mStatsManager?.addUserStats(uid, isLocal)
                if (mStatsManager?.isEnabled == true) {
                    mHandler?.removeCallbacks(this)
                    mHandler?.postDelayed(this, STATS_REFRESH_INTERVAL.toLong())
                }
            }
            requestGridLayout()
        }
    }

    private fun createVideoView(surface: TextureView?): ViewGroup {
        val layout = RelativeLayout(context)
        layout.id = surface.hashCode()
        val videoLayoutParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layout.addView(surface, videoLayoutParams)
        val text = TextView(context)
        text.id = layout.hashCode()
        val textParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        textParams.bottomMargin = mStatMarginBottom
        textParams.leftMargin = STAT_LEFT_MARGIN
        text.setTextColor(Color.WHITE)
        text.textSize = STAT_TEXT_SIZE.toFloat()
        layout.addView(text, textParams)
        return layout
    }

    fun removeUserVideo(uid: Int, isLocal: Boolean) {
        if (isLocal && mUidList.contains(0)) {
            mUidList.remove(0)
            mUserViewList.remove(0)
        } else if (mUidList.contains(uid)) {
            mUidList.remove(uid)
            mUserViewList.remove(uid)
        }
        mStatsManager?.removeUserStats(uid)
        requestGridLayout()
        if (childCount == 0) {
            mHandler?.removeCallbacks(this)
        }
    }

    private fun requestGridLayout() {
        removeAllViews()
        layout(mUidList.size)
    }

    private fun layout(size: Int) {
        val params: Array<RelativeLayout.LayoutParams?> = getParams(size)
        for (i in 0 until size) {
            addView(mUserViewList.get(mUidList[i]), params[i])
        }
    }

    private fun getParams(size: Int): Array<RelativeLayout.LayoutParams?> {
        val width: Int = measuredWidth
        val height: Int = measuredHeight
        val array: Array<RelativeLayout.LayoutParams?> =
            arrayOfNulls<RelativeLayout.LayoutParams>(size)
        for (i in 0 until size) {
            when (i) {
                0 -> {
                    array[0] = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                    )
                    array[0]?.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                    array[0]?.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE)
                }
                1 -> {
                    array[1] = RelativeLayout.LayoutParams(width, height / 2)
                    array[0]?.height = array[1]?.height
                    array[1]?.addRule(RelativeLayout.BELOW, mUserViewList.get(mUidList[0]).id)
                    array[1]?.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE)
                }
                2 -> {
                    array[i] = RelativeLayout.LayoutParams(width / 2, height / 2)
                    array[i - 1]?.width = array[i]?.width
                    array[i]?.addRule(
                        RelativeLayout.RIGHT_OF,
                        mUserViewList.get(mUidList[i - 1]).id
                    )
                    array[i]?.addRule(
                        RelativeLayout.ALIGN_TOP,
                        mUserViewList.get(mUidList[i - 1]).id
                    )
                }
                3 -> {
                    array[i] = RelativeLayout.LayoutParams(width / 2, height / 2)
                    array[0]?.width = width / 2
                    array[1]?.addRule(RelativeLayout.BELOW, 0)
                    array[1]?.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
                    array[1]?.addRule(RelativeLayout.RIGHT_OF, mUserViewList.get(mUidList[0]).id)
                    array[1]?.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                    array[2]?.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE)
                    array[2]?.addRule(RelativeLayout.RIGHT_OF, 0)
                    array[2]?.addRule(RelativeLayout.ALIGN_TOP, 0)
                    array[2]?.addRule(RelativeLayout.BELOW, mUserViewList.get(mUidList[0]).id)
                    array[3]?.addRule(RelativeLayout.BELOW, mUserViewList.get(mUidList[1]).id)
                    array[3]?.addRule(RelativeLayout.RIGHT_OF, mUserViewList.get(mUidList[2]).id)
                }
            }
        }
        return array
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearAllVideo()
    }

    private fun clearAllVideo() {
        removeAllViews()
        mUserViewList.clear()
        mUidList.clear()
        mHandler?.removeCallbacks(this)
    }

    override fun run() {
        if (mStatsManager != null && mStatsManager?.isEnabled == true) {
            val count: Int = childCount
            for (i in 0 until count) {
                val layout: RelativeLayout = getChildAt(i) as RelativeLayout
                val text: TextView = layout.findViewById<TextView>(layout.hashCode())
                if (text != null) {
                    val data: StatsData = mStatsManager?.getStatsData(mUidList[i])!!
                    val info: String? = if (data != null) data.toString() else null
                    if (info != null) text.text = info
                }
            }
            mHandler?.postDelayed(this, STATS_REFRESH_INTERVAL.toLong())
        }
    }

    companion object {
        private const val MAX_USER = 4
        private const val STATS_REFRESH_INTERVAL = 2000
        private const val STAT_LEFT_MARGIN = 34
        private const val STAT_TEXT_SIZE = 10
    }
}