package com.datn.apptravel.ui.discover.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.ExploreItem

class ExploreAdapter(
    private val items: List<ExploreItem>
) : RecyclerView.Adapter<ExploreAdapter.ExploreViewHolder>() {

    inner class ExploreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgExplore)
        val title: TextView = view.findViewById(R.id.tvExploreTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_explore, parent, false)
        return ExploreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val item = items[position]
        holder.img.setImageResource(item.imageRes)
        holder.title.text = item.title
    }

    override fun getItemCount(): Int = items.size
}
