package com.herfy.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.herfy.R

class LanguageAdapter(
    private val context: Context,
    private val resource: Int,
    private val objects: List<String>,
    private var hidingItemIndex: Int,
    private val textViewResource : Int
) : ArrayAdapter<String>(context,resource, textViewResource, objects) {

    val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
        var view: View? = convertView
        if (view == null) {
            view = inflater.inflate(resource, container, false)
        }

        view?.let {
            val tvText : TextView = it.findViewById(R.id.text1)
            tvText.text = getItem(position)
        }


        return view!!
    }


    fun setItemToHide(itemToHide: Int) {
        hidingItemIndex = itemToHide
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var view: View? = null



        if (position == hidingItemIndex) {
            val tv = TextView(getContext())
            tv.height = 0
            tv.visibility = View.GONE
            view = tv
        }else {
            if (view == null) {
                view = inflater.inflate(resource, parent, false)
            }
            view?.let {
                val tvText : TextView = it.findViewById(R.id.text1)
                tvText.text = getItem(position)
            }
        }

        return view
    }


}