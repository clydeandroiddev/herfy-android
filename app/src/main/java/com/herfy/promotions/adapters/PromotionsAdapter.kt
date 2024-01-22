package com.herfy.promotions.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.herfy.constants.Values
import com.herfy.databinding.ItemPromotionsBinding
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.herfy.util.CacheUtil
import java.util.Locale

/**
 * [PromotionsAdapter] adapter for displaying all the menu categories and items
 */
class PromotionsAdapter(
    private var campaigns: Array<Campaign>,
    private var onSelect: (Campaign) -> Unit
) : RecyclerView.Adapter<PromotionsAdapter.CampaignViewHolder>() {

    /**
     * [CampaignViewHolder] for binding the views
     * @param binding as [ItemPromotionsBinding] the views to be binded
     */
    class CampaignViewHolder(
        private val binding: ItemPromotionsBinding,
        private val onSelect: (Campaign) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        /**
         * bind the [Campaign] to display
         */
        fun bind(campaign: Campaign) {

            if (CacheUtil.getInstance().campaign != null) {
                if (campaign.id.equals(CacheUtil.getInstance().campaign.id, ignoreCase = true)) {
                    binding.card.strokeWidth = 2
                } else {
                    binding.card.strokeWidth = 0
                }
            } else {
                binding.card.strokeWidth = 0
            }

            binding.card.setOnClickListener {
                onSelect(campaign)
            }

            binding.digitalName.text = campaign.attributes.name

            binding.digitalDesc.text = campaign.attributes.description

            var imageUrl = campaign.attributes.imageUri
            if (imageUrl == null || imageUrl.equals(
                    "null",
                    ignoreCase = true
                ) || imageUrl.isEmpty() || imageUrl.lowercase(
                    Locale.getDefault()
                ).contains("default-image.png")
            ) {
                imageUrl = Values.DEFAULT_IMAGE_URL
            }

            Glide.with(itemView.context).load(imageUrl).skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.digitalImage.visibility = View.INVISIBLE
                        binding.progressImage.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.digitalImage.visibility = View.VISIBLE
                        binding.progressImage.visibility = View.GONE
                        return false
                    }
                }).into(binding.digitalImage)

        }
    }

    /**
     * Setup the binding for the view holder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {

        /**
         * inflates the current layout with view holder
         */
        ItemPromotionsBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            return CampaignViewHolder(it, onSelect)
        }
    }

    /**
     * Binds the data to the view
     */
    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(campaigns[position])
    }

    /**
     * Returns the number of menu categories
     */
    override fun getItemCount(): Int = campaigns.size

    fun updateCampaigns(campaigns: Array<Campaign>) {
        this.campaigns = campaigns
        notifyDataSetChanged()
    }

}