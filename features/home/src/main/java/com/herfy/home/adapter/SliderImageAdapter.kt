package com.herfy.home.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.home.R
import com.herfy.home.databinding.ItemBannerHomeBinding
import com.skylinedynamics.solosdk.api.models.objects.Slide

class SliderImageAdapter(var imageList : List<Slide>? = null, val sliderListener: SliderListener) : RecyclerView.Adapter<SliderImageAdapter.SliderImageViewHolder>(){


    inner class SliderImageViewHolder(val binding : ItemBannerHomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderImageViewHolder {
        val binding : ItemBannerHomeBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_banner_home,
            parent,
            false
        )
        return SliderImageViewHolder(binding)
    }

    override fun getItemCount(): Int = imageList?.size?: 0

    override fun onBindViewHolder(holder: SliderImageViewHolder, position: Int) {
        holder.binding.apply {
            imageList?.let {
                slide = it[position]
                listener = sliderListener
                executePendingBindings()
            }

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateImageList(imageList: ArrayList<Slide>){
        this.imageList = imageList
        notifyDataSetChanged()
    }


    interface SliderListener{
        fun onSelectedImage(slide: Slide)
    }

}

