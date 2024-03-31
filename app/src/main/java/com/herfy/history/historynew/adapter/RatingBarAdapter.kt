package com.herfy.history.historynew.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.herfy.history.databinding.ItemRatingStarOrderBinding
import com.skylinedynamics.solosdk.api.models.objects.Rating

class RatingBarAdapter(
    val items: List<Rating> = arrayListOf(),
) : RecyclerView.Adapter<RatingBarAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemRatingStarOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemRatingStarOrderBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.history.R.layout.item_rating_star_order,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    fun getRatings(): List<Rating> {
        return items
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            executePendingBindings()
            ratingName.text = item.attributes.name
            ratingStar.setOnRatingChangeListener { ratingBar, rating, fromUser ->
                run {
                    items[position].attributes.rating = rating.toInt()
                }
            }
        }
    }
}