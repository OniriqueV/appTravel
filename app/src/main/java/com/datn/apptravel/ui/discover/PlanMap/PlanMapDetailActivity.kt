package com.datn.apptravel.ui.discover.PlanMap

import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.PlanMap.adapter.StoryAdapter
import com.datn.apptravel.ui.discover.PlanMap.bottomsheet.PlanMapDetailCommentBottomSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlanMapDetailActivity : AppCompatActivity() {

    private val viewModel: PlanMapDetailViewModel by viewModel()

    // 🔥 Shared VM để bắn signal về Feed

    private lateinit var vpStory: ViewPager2
    private lateinit var btnBack: ImageView
    private lateinit var btnLike: ImageView
    private lateinit var btnComment: ImageView

    private lateinit var tvTitle: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvLikeCount: TextView
    private lateinit var tvCommentCount: TextView

    private lateinit var storyAdapter: StoryAdapter

    private var currentPlanId: String = ""
    private var currentTripId: String? = null   // 🔥 FIX: lấy từ Intent

    // 🔥 dùng để tính delta like
    private var lastLikeCount: Int? = null

    /* =========================
       STORY CONFIG
       ========================= */
    private val STORY_DURATION = 3000L

    private val autoHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var autoRunnable: Runnable? = null

    private var storyCount = 0

    private lateinit var gestureDetector: android.view.GestureDetector

    private lateinit var layoutStoryProgress: LinearLayout
    private val progressBars = mutableListOf<ProgressBar>()
    private var progressAnimator: android.animation.ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_map_detail)

        currentPlanId = intent.getStringExtra("planId") ?: run {
            finish(); return
        }
        currentTripId = intent.getStringExtra("tripId") // 🔥 FIX

        bindViews()
        setupStory()
        setupGesture()
        setupActions()
        observeViewModel()

        viewModel.loadPlan(currentPlanId)
    }

    /* =========================
       BIND VIEW
       ========================= */
    private fun bindViews() {
        vpStory = findViewById(R.id.vpStory)
        btnBack = findViewById(R.id.btnBack)
        layoutStoryProgress = findViewById(R.id.layoutStoryProgress)
        btnLike = findViewById(R.id.btnLike)
        btnComment = findViewById(R.id.btnComment)

        tvTitle = findViewById(R.id.tvTitle)
        tvLocation = findViewById(R.id.tvLocation)
        tvLikeCount = findViewById(R.id.tvLikeCount)
        tvCommentCount = findViewById(R.id.tvCommentCount)
    }

    /* =========================
       STORY SETUP
       ========================= */
    private fun setupStory() {
        storyAdapter = StoryAdapter()
        vpStory.adapter = storyAdapter

        vpStory.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    startAutoStory()
                }
            }
        )
    }

    /* =========================
       ACTIONS
       ========================= */
    private fun setupActions() {
        btnBack.setOnClickListener { finish() }

        btnLike.setOnClickListener {
            viewModel.toggleLike()
        }

        btnComment.setOnClickListener {
            openCommentSheet(currentPlanId)
        }
    }

    /* =========================
       OBSERVE VIEWMODEL
       ========================= */
    private fun observeViewModel() {
        viewModel.plan.observe(this) { plan ->

            // ===================================

            // STORY
            storyAdapter.submit(plan.images)
            storyCount = plan.images.size
            setupStoryProgress(storyCount)
            startAutoStory()

            // INFO
            tvTitle.text = plan.title
            tvLocation.text = listOfNotNull(
                plan.location,
                plan.address
            ).joinToString(" • ")

            // LIKE / COMMENT
            tvLikeCount.text = plan.likeCount.toString()
            tvCommentCount.text = plan.commentCount.toString()

            btnLike.setImageResource(
                if (plan.liked)
                    R.drawable.ic_heart_filled
                else
                    R.drawable.ic_heart_outline
            )
        }
    }

    /* =========================
       GESTURE
       ========================= */
    private fun setupGesture() {
        gestureDetector = android.view.GestureDetector(
            this,
            object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    stopAutoStory()

                    val width = vpStory.width
                    val x = e.x

                    if (x > width / 2) {
                        goNextStory()
                    } else {
                        goPrevStory()
                    }
                    return true
                }
            }
        )

        vpStory.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                gestureDetector.onTouchEvent(event)
            }
            false
        }
    }

    /* =========================
       AUTO STORY
       ========================= */
    private fun startAutoStory() {
        if (storyCount <= 0) return

        stopAutoStory()

        val index = vpStory.currentItem
        animateProgress(index)

        autoRunnable = Runnable {
            goNextStory()
        }

        autoHandler.postDelayed(autoRunnable!!, STORY_DURATION)
    }

    private fun stopAutoStory() {
        autoRunnable?.let { autoHandler.removeCallbacks(it) }
    }

    private fun goNextStory() {
        stopAutoStory()

        val next = vpStory.currentItem + 1
        if (next < storyCount) {
            vpStory.setCurrentItem(next, true)
            startAutoStory()
        } else {
            finish()
        }
    }

    private fun goPrevStory() {
        stopAutoStory()

        val prev = vpStory.currentItem - 1
        if (prev >= 0) {
            vpStory.setCurrentItem(prev, true)
            startAutoStory()
        }
    }

    private fun setupStoryProgress(count: Int) {
        layoutStoryProgress.removeAllViews()
        progressBars.clear()

        repeat(count) {
            val progress = ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    marginEnd = 8
                }
                max = 100
                progress = 0
                progressDrawable =
                    ContextCompat.getDrawable(
                        this@PlanMapDetailActivity,
                        R.drawable.story_progress_drawable
                    )
            }

            progressBars.add(progress)
            layoutStoryProgress.addView(progress)
        }
    }

    private fun animateProgress(index: Int) {
        progressAnimator?.cancel()

        progressBars.forEachIndexed { i, bar ->
            bar.progress = if (i < index) 100 else 0
        }

        if (index !in progressBars.indices) return

        progressAnimator = android.animation.ValueAnimator.ofInt(0, 100).apply {
            duration = STORY_DURATION
            addUpdateListener {
                progressBars[index].progress = it.animatedValue as Int
            }
            start()
        }
    }

    /* =========================
       COMMENT
       ========================= */
    private fun openCommentSheet(planId: String) {
        PlanMapDetailCommentBottomSheet.show(supportFragmentManager)
    }

    override fun onPause() {
        super.onPause()
        stopAutoStory()
    }

    override fun onResume() {
        super.onResume()
        if (storyCount > 0) startAutoStory()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoStory()
        autoHandler.removeCallbacksAndMessages(null)
    }
}
