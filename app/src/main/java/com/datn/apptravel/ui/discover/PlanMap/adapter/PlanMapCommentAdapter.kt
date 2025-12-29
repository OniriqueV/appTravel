package com.datn.apptravel.ui.discover.PlanMap.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.PlanCommentDto

class PlanMapCommentAdapter : RecyclerView.Adapter<PlanMapCommentAdapter.VH>() {

    private val items = mutableListOf<PlanCommentDto>()

    fun submit(list: List<PlanCommentDto>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plan_map_comment, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvUser.text = item.userId
        holder.tvContent.text = item.content
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvUser: TextView = view.findViewById(R.id.tvUser)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
    }
}