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
    private val onAddPhotoClick: () -> Unit,
    private val onDeletePhotoClick: (String, Int) -> Unit

) : RecyclerView.Adapter<PhotoCollectionAdapter.PhotoViewHolder>() {
    private var isReadOnly = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoCollectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        // First position is always the "Add Photo" button
        if ( !isReadOnly && position == 0) {
            holder.bind("", true, -1)
        } else {
            val photoIndex = if (isReadOnly) position else position - 1
            holder.bind(photos[photoIndex], false, photoIndex)
        }
    }

    override fun getItemCount(): Int {
        return if (isReadOnly) {
            photos.size          // ❌ KHÔNG +1
        } else {
            photos.size + 1      // ✅ có nút +
        }
    } // +1 for add button at front

    fun updatePhotos(newPhotos: List<String>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }
    
    fun removePhoto(position: Int) {
        if (position >= 0 && position < photos.size) {
            photos.removeAt(position)
            notifyItemRemoved(position + 1) // +1 because of add button at position 0
            notifyItemRangeChanged(position + 1, photos.size)
        }
    }

    fun setReadOnly(readOnly: Boolean) {
        isReadOnly = readOnly
        notifyDataSetChanged()
    }


    inner class PhotoViewHolder(
        private val binding: ItemPhotoCollectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoFileName: String, isAddButton: Boolean, photoIndex: Int) {
            if (isAddButton) {
                binding.ivPhoto.visibility = View.GONE
                binding.btnDeletePhoto.visibility = View.GONE

                if (isReadOnly) {
                    binding.layoutAddPhoto.visibility = View.GONE
                } else {
                        binding.layoutAddPhoto.visibility = View.VISIBLE
                        binding.layoutAddPhoto.setOnClickListener {
                            onAddPhotoClick()
                        }
                    }
            } else {
                binding.ivPhoto.visibility = View.VISIBLE
                binding.layoutAddPhoto.visibility = View.GONE
                binding.btnDeletePhoto.visibility = View.GONE

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
                if (isReadOnly) {
                    // 🔒 READ ONLY → KHÔNG CHO XOÁ
                    binding.btnDeletePhoto.visibility = View.GONE
                    binding.btnDeletePhoto.setOnClickListener(null)
                } else {
                // Delete button click
                    binding.btnDeletePhoto.visibility = View.VISIBLE
                    binding.btnDeletePhoto.setOnClickListener {
                        onDeletePhotoClick(photoFileName, photoIndex)
                }
            }
        }
    }
}}