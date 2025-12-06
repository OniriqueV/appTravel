package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.databinding.ItemPhotoCollectionBinding
import com.datn.apptravel.utils.ApiConfig

class PhotoCollectionAdapter(
    private val photos: MutableList<String>,
    private val onAddPhotoClick: () -> Unit
) : RecyclerView.Adapter<PhotoCollectionAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoCollectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        if (position < photos.size) {
            holder.bind(photos[position], false)
        } else {
            holder.bind("", true)
        }
    }

    override fun getItemCount(): Int = photos.size + 1 // +1 for add button

    fun updatePhotos(newPhotos: List<String>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoCollectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoFileName: String, isAddButton: Boolean) {
            if (isAddButton) {
                binding.ivPhoto.visibility = View.GONE
                binding.layoutAddPhoto.visibility = View.VISIBLE
                binding.layoutAddPhoto.setOnClickListener {
                    onAddPhotoClick()
                }
            } else {
                binding.ivPhoto.visibility = View.VISIBLE
                binding.layoutAddPhoto.visibility = View.GONE

                // Load image from server using Glide
                val imageUrl = ApiConfig.getImageUrl(photoFileName)
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_a)
                    .error(R.drawable.bg_a)
                    .centerCrop()
                    .into(binding.ivPhoto)

                binding.ivPhoto.setOnClickListener {
                    // TODO: Open fullscreen photo viewer
                }
            }
        }
    }
}