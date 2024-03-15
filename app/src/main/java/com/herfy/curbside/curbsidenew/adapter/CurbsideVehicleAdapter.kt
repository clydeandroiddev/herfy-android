package com.herfy.curbside.curbsidenew.adapter

import android.R
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.herfy.common.formatDate
import com.herfy.ordertype.databinding.ItemCurbsideNewBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.isVisible
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar


class CurbsideVehicleAdapter(
    val curbsideList: ArrayList<CustomerCar> = arrayListOf(),
    val curbsideListener: CurbsideListener,
    var selectedId: String,
    val fromProfile: Boolean
) : RecyclerView.Adapter<CurbsideVehicleAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemCurbsideNewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemCurbsideNewBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            com.herfy.ordertype.R.layout.item_curbside_new,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = curbsideList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = curbsideList[position]
        holder.binding.apply {

            val colorStateListSelected = ColorStateList.valueOf(
                ContextCompat.getColor(
                    curbsideEllipse.context, R.color.white
                )
            )

            if (fromProfile || selectedId != item.id) {
                curbsideEllipse.imageTintList = null
                chooseCurbside.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_circle_unselected)
            } else {
                curbsideEllipse.imageTintList = colorStateListSelected
                chooseCurbside.setBackgroundResource(com.herfy.ordertype.R.drawable.bg_deliver_selected)
                selectedIdentifier.setImageResource(com.herfy.ordertype.R.drawable.ic_store_selected)
            }

            if (fromProfile) {
                editItem.isVisible()
                deleteItem.isVisible()
                selectedIdentifier.gone()
            } else {
                editItem.gone()
                selectedIdentifier.isVisible()
                deleteItem.gone()
            }

            curbsidePlateNumber.text = item.attributes.plateNumber

            chooseCurbside.setOnClickListener {
                curbsideListener.onSelectedCar(item, position)
            }

            editItem.setOnClickListener {
                curbsideListener.onEditItem(item, position)
            }

            deleteItem.setOnClickListener {
                curbsideListener.onDeleteItem(item, position)
            }

            Glide.with(curbsideBrand.context).load(item.attributes.carMakerImageUri)
                .apply(RequestOptions().override(80, 80)).skipMemoryCache(false).diskCacheStrategy(
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

            curbsideAdded.text =
                CacheUtil.getInstance().getTranslations(Text.ADDED_CURBSIDE, "Added: (x)")
                    .replace("(x)", item.attributes.createdAt.formatDate())

            executePendingBindings()
        }
    }

    fun updateList(promoList: ArrayList<CustomerCar>) {
        this.curbsideList.clear()
        this.curbsideList.addAll(promoList)
        notifyDataSetChanged()
    }

    fun getCar(position: Int): CustomerCar {
        return this.curbsideList[position]
    }
    fun removeItem(position: Int) {
        this.curbsideList.removeAt(position)
        notifyDataSetChanged()
    }

    fun listCount(): Int {
        return this.curbsideList.size
    }

    fun updateSelectedId(id: String) {
        this.selectedId = id
        notifyDataSetChanged()
    }

    interface CurbsideListener {
        fun onSelectedCar(car: CustomerCar, position: Int)
        fun onDeleteItem(car: CustomerCar, position: Int)
        fun onEditItem(car: CustomerCar, position: Int)
    }
}