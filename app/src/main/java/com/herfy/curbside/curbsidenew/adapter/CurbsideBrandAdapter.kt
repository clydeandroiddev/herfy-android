package com.herfy.curbside.curbsidenew.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.herfy.ordertype.databinding.ItemVehicleBrandBinding
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker

class CurbsideBrandAdapter(
    val curbsideList: ArrayList<CarMaker> = arrayListOf(),
    val curbsideListener: CurbsideListener,
    var selectedId: String
) : RecyclerView.Adapter<CurbsideBrandAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemVehicleBrandBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemVehicleBrandBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.ordertype.R.layout.item_vehicle_brand,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = curbsideList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = curbsideList[position]
        holder.binding.apply {

            if (selectedId != item.id) {
                chooseCurbside.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_circle_unselected)
            } else {
                chooseCurbside.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver_selected)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_store_selected)
            }

            brandName.text = item.attributes.name

            Glide.with(curbsideBrand.context).load(item.attributes.imageUri)
                .skipMemoryCache(false).diskCacheStrategy(
                    DiskCacheStrategy.ALL
                ).apply(RequestOptions.centerInsideTransform())
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                }).into(curbsideBrand)

            chooseCurbside.setOnClickListener {
                curbsideListener.onSelectedCar(item, position)
            }

            executePendingBindings()
        }
    }

    fun updateList(promoList: ArrayList<CarMaker>) {
        this.curbsideList.clear()
        this.curbsideList.addAll(promoList)
        notifyDataSetChanged()
    }

    fun updateSelectedId(id: String) {
        this.selectedId = id
        notifyDataSetChanged()
    }

    interface CurbsideListener {
        fun onSelectedCar(car: CarMaker, position: Int)
    }
}