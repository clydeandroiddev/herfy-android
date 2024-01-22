package com.herfy.account.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class HintAdapter: ArrayAdapter<String> {

    constructor(context: Context, resource: Int) : super(context, resource) {}

    constructor(context: Context, resource: Int, objects: List<String>) : super(
        context, resource, objects
    )

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = if (position == 0) {
            val tv = TextView(context)
            tv.height = 0
            tv.visibility = View.GONE
            tv
        } else {
            super.getDropDownView(position, null, parent)
        }
        parent.isVerticalScrollBarEnabled = false
        return v
    }

}