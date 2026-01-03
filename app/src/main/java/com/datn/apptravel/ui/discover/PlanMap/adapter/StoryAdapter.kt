package com.datn.apptravel.ui.discover.PlanMap.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.util.ImageUrlUtil

class StoryAdapter : RecyclerView.Adapter<StoryAdapter.VH>() {

    private val images = mutableListOf<String>()

    fun submit(list: List<String>) {
        images.clear()
        images.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(
            LayoutInflater.from(p.context)
                .inflate(R.layout.item_story_image, p, false)
        )

    override fun getItemCount() = images.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val url = ImageUrlUtil.toFullUrl(images[i])

        // Background
        h.bg.load(url) {
            crossfade(true)
        }

        // Main image
        h.image.load(url) {
            crossfade(true)
            placeholder(R.drawable.ic_image_placeholder)
            error(R.drawable.ic_image_placeholder)
        }
    }


    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivStory)
        val bg: ImageView = view.findViewById(R.id.imgBg)
    }
}