package com.herfy.auth.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.herfy.R
import com.skylinedynamics.solosdk.api.models.objects.AllowedCountries

class AllowedCountriesAdapter(val context: Context, var countries: ArrayList<AllowedCountries>): BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int = countries.size

    override fun getItem(position: Int): Any {
        return countries[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = inflater.inflate(R.layout.item_spinner, parent, false)
        val code = view.findViewById(R.id.spinnerText) as TextView

        var dialCode = countries[position].dialCode
        code.text = if(dialCode.contains("+")) {
            dialCode
        } else "+$dialCode"

        return view
    }
}