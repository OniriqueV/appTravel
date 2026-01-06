package com.datn.apptravels.ui.discover.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.util.ImageUrlUtil
import com.datn.apptravels.ui.discover.model.User
class FollowerAdapter(
    private val onAddClick: (User) -> Unit
) : RecyclerView.Adapter<FollowerAdapter.VH>() {

    private val items = mutableListOf<User>()

    fun submit(list: List<User>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_add_follower, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val btnAdd: ImageButton = view.findViewById(R.id.btnAdd)

        fun bind(user: User) {
            tvName.text = user.userName

            Glide.with(imgAvatar)
                .load(ImageUrlUtil.toFullUrl(user.avatarUrl))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(imgAvatar)

            btnAdd.setOnClickListener {
                onAddClick(user)
            }
        }
    }
}
