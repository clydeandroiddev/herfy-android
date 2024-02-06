package com.herfy.menu.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.herfy.R
import com.herfy.menu.MenuContract
import com.herfy.menu.viewholders.FrequentBoughtViewHolder
import io.shipbook.shipbooksdk.Log

class FrequentBoughItemsAdapter(private val context: Context,private val menuPresenter :MenuContract.Presenter): RecyclerView.Adapter<FrequentBoughtViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrequentBoughtViewHolder {
        return FrequentBoughtViewHolder(
            context, menuPresenter,
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_modifier_frequently_bought, parent, false)
        )
    }

    override fun getItemCount(): Int {
       return menuPresenter.frequentBoughtItemCount
    }

    override fun onBindViewHolder(holder: FrequentBoughtViewHolder, position: Int) {
        return menuPresenter.onBindFrequentBoughtItemPosition(position,holder)
    }
}
