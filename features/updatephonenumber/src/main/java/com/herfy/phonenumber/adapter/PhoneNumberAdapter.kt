package com.herfy.phonenumber.adapter

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.herfy.phonenumber.R
import com.herfy.phonenumber.model.CountriesUIModel

class PhoneNumberAdapter(
    private val context: Context,
    private val resource: Int,
    private val objects: List<CountriesUIModel>,
    private var hidingItemIndex: Int = 0,
    private val textViewResource : Int
) : ArrayAdapter<CountriesUIModel>(context, resource, textViewResource, objects) {


    val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
        var view: View? = convertView
        if (view == null) {
            view = inflater.inflate(resource, container, false)
        }

        view?.let {
            val tvFlag : TextView = it.findViewById(R.id.tv_flag)
            val tvText : TextView = it.findViewById(R.id.txtPrefix)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tvFlag.text =
                    Html.fromHtml(getItem(position)!!.flag, Html.FROM_HTML_MODE_LEGACY)
            }else {
                tvFlag.text =
                    Html.fromHtml(getItem(position)!!.flag)
            }
            tvText.text = getItem(position)!!.dialCode
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
                val tvFlag : TextView = it.findViewById(R.id.tv_flag)
                val tvText : TextView = it.findViewById(R.id.txtPrefix)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    tvFlag.text =
                        Html.fromHtml(getItem(position)!!.flag, Html.FROM_HTML_MODE_LEGACY)
                }else {
                    tvFlag.text =
                        Html.fromHtml(getItem(position)!!.flag)
                }
                tvText.text = getItem(position)!!.dialCode
            }
        }

        return view
    }


}