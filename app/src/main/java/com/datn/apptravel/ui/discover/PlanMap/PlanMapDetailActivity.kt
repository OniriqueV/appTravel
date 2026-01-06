package com.datn.apptravels.ui.discover.PlanMap

import android.animation.ValueAnimator
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.PlanMap.adapter.StoryAdapter
import com.datn.apptravels.ui.discover.PlanMap.bottomsheet.PlanMapDetailCommentBottomSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlanMapDetailActivity :
    AppCompatActivity(),
    PlanMapDetailCommentBottomSheet.CommentSheetListener {

    private val viewModel: PlanMapDetailViewModel by viewModel()

    private lateinit var vpStory: ViewPager2
    private lateinit var gestureLayer: View
    private lateinit var btnBack: ImageView
    private lateinit var btnLike: ImageView
    private lateinit var btnComment: ImageView

    private lateinit var tvTitle: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvLikeCount: TextView
    private lateinit var tvCommentCount: TextView

    private lateinit var storyAdapter: StoryAdapter

    private val STORY_DURATION = 6000L

    private val autoHandler = Handler(Looper.getMainLooper())
    private var autoRunnable: Runnable? = null

    private var storyCount = 0
    private var pausedProgress = 0
    private var isCommentOpen = false

    /* HOLD TO PAUSE */
    private var isHolding = false
    private val holdHandler = Handler(Looper.getMainLooper())
    private val holdRunnable = Runnable {
        isHolding = true
        pauseStory()
    }

    private lateinit var layoutStoryProgress: LinearLayout
    private val progressBars = mutableListOf<ProgressBar>()
    private var progressAnimator: ValueAnimator? = null

    private var downY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_map_detail)

        bindViews()
        setupStory()
        setupGesture()
        setupActions()
        observeViewModel()

        intent.getStringExtra("planId")?.let {
            viewModel.loadPlan(it)
        } ?: finish()
    }

    private fun bindViews() {
        vpStory = findViewById(R.id.vpStory)
        gestureLayer = findViewById(R.id.gestureLayer)
        btnBack = findViewById(R.id.btnBack)
        btnLike = findViewById(R.id.btnLike)
        btnComment = findViewById(R.id.btnComment)
        layoutStoryProgress = findViewById(R.id.layoutStoryProgress)

        tvTitle = findViewById(R.id.tvTitle)
        tvLocation = findViewById(R.id.tvLocation)
        tvLikeCount = findViewById(R.id.tvLikeCount)
        tvCommentCount = findViewById(R.id.tvCommentCount)
    }

    private fun setupStory() {
        storyAdapter = StoryAdapter()
        vpStory.adapter = storyAdapter

        vpStory.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    pausedProgress = 0
                    startAutoStory()
                }
            }
        )
    }

    private fun observeViewModel() {
        viewModel.storyImages.observe(this) { images ->
            storyAdapter.submit(images)
            storyCount = images.size
            setupStoryProgress(storyCount)
            vpStory.setCurrentItem(0, false)
            startAutoStory()
        }

        viewModel.plan.observe(this) { plan ->

            tvTitle.text = plan.title
            tvLocation.text = listOfNotNull(plan.location, plan.address)
                .joinToString(" • ")

            tvLikeCount.text = plan.likeCount.toString()
            tvCommentCount.text = plan.commentCount.toString()

            // 🔥🔥🔥 BẮT BUỘC PHẢI CÓ
            btnLike.setImageResource(
                if (plan.liked)
                    R.drawable.ic_heart_filled
                else
                    R.drawable.ic_heart_outline
            )
        }
    }

    private fun setupActions() {
        btnBack.setOnClickListener { finish() }
        btnLike.setOnClickListener { viewModel.toggleLike() }
        btnComment.setOnClickListener {
            PlanMapDetailCommentBottomSheet.show(supportFragmentManager)
        }
    }

    /* =========================
       GESTURE (BẮT TRÊN gestureLayer)
       ========================= */
    private fun setupGesture() {

        gestureLayer.setOnTouchListener { _, event ->
            if (isCommentOpen) {
                return@setOnTouchListener false
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.y
                    isHolding = false
                    holdHandler.postDelayed(holdRunnable, 180)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.y - downY
                    if (deltaY > 120.dp) finish()
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    holdHandler.removeCallbacks(holdRunnable)

                    if (isHolding) {
                        isHolding = false
                        resumeStory()
                    } else {
                        if (event.x > gestureLayer.width / 2) {
                            goNextStory()
                        } else {
                            goPrevStory()
                        }
                    }
                    true
                }

                else -> false
            }
        }

    }

    /* =========================
       AUTO STORY
       ========================= */
    private fun startAutoStory() {
        if (storyCount <= 0 || isCommentOpen) return

        stopAutoStory()
        pausedProgress = 0
        animateProgress(vpStory.currentItem)

        autoRunnable = Runnable { goNextStory() }
        autoHandler.postDelayed(autoRunnable!!, STORY_DURATION)
    }

    private fun pauseStory() {
        autoRunnable?.let { autoHandler.removeCallbacks(it) }
        progressAnimator?.let {
            pausedProgress =
                progressBars.getOrNull(vpStory.currentItem)?.progress ?: 0
            it.cancel()
        }
    }

    private fun resumeStory() {
        if (pausedProgress >= 100 || isCommentOpen) return

        animateProgress(vpStory.currentItem)

        autoRunnable = Runnable { goNextStory() }
        val remain = STORY_DURATION * (100 - pausedProgress) / 100
        autoHandler.postDelayed(autoRunnable!!, remain)
    }

    private fun stopAutoStory() {
        autoRunnable?.let { autoHandler.removeCallbacks(it) }
        autoRunnable = null
        progressAnimator?.cancel()
    }

    private fun goNextStory() {
        stopAutoStory()
        val next = vpStory.currentItem + 1
        if (next < storyCount) {
            vpStory.setCurrentItem(next, true)
        } else {
            finish()
        }
    }

    private fun goPrevStory() {
        stopAutoStory()
        val prev = vpStory.currentItem - 1
        if (prev >= 0) {
            vpStory.setCurrentItem(prev, true)
        }
    }

    /* =========================
       STORY PROGRESS
       ========================= */
    private fun setupStoryProgress(count: Int) {
        layoutStoryProgress.removeAllViews()
        progressBars.clear()

        repeat(count) {
            val bar = ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    4.dp,
                    1f
                ).apply { marginEnd = 8 }

                max = 100
                progress = 0

                progressDrawable = ContextCompat.getDrawable(
                    this@PlanMapDetailActivity,
                    R.drawable.story_progress_drawable
                )
            }

            progressBars.add(bar)
            layoutStoryProgress.addView(bar)
        }
    }

    private fun animateProgress(index: Int) {
        progressAnimator?.cancel()

        progressBars.forEachIndexed { i, bar ->
            bar.progress = if (i < index) 100 else 0
        }

        progressAnimator = ValueAnimator.ofInt(pausedProgress, 100).apply {
            duration = STORY_DURATION * (100 - pausedProgress) / 100
            addUpdateListener {
                progressBars[index].progress = it.animatedValue as Int
            }
            start()
        }
    }

    override fun onCommentSheetShown() {
        isCommentOpen = true
        pauseStory()
        gestureLayer.isEnabled = false
        gestureLayer.visibility = View.GONE
    }

    override fun onCommentSheetDismissed() {
        isCommentOpen = false
        resumeStory()
        gestureLayer.isEnabled = true
        gestureLayer.visibility = View.VISIBLE
    }
}

private val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
