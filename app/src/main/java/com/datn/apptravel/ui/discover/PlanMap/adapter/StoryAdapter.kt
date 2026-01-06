package com.datn.apptravels.ui.discover.PlanMap.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.util.ImageUrlUtil

class StoryAdapter : RecyclerView.Adapter<StoryAdapter.VH>() {

    private val images = mutableListOf<String>()

    fun submit(list: List<String>) {
        images.clear()
        images.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story_image, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = images.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = ImageUrlUtil.toFullUrl(images[position])

        holder.bg.load(url) {
            crossfade(true)
        }

        holder.image.load(url) {
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
