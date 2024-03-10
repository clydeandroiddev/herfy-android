package com.herfy.faq.adapter

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView

class FrequencyStatusLookUp(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<FrequencyAskUIModel>() {

    override fun getItemDetails(event: MotionEvent): ItemDetails<FrequencyAskUIModel>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (
                recyclerView.getChildViewHolder(view) as
                    FrequencyAskAdapter.FrequentViewHolder
                )
                .getItemDetails()
        }
        return null
    }
}