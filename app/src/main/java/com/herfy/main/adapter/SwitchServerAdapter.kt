package com.herfy.main.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.herfy.base.BaseActivity
import com.herfy.databinding.ItemSwitchServerBinding
import com.herfy.ext.cast
import com.herfy.utils.helper.*


class SwitchServerAdapter(private val serverList: List<com.herfy.model.ServerType>, private var lastSelectedPos: Int = -1, private val callback: (com.herfy.model.ServerType) -> Unit) :
    RecyclerView.Adapter<SwitchServerAdapter.SwitchServerViewHolder>() {

    private var customUrl = ""
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwitchServerViewHolder {
        val binding: ItemSwitchServerBinding = ItemSwitchServerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SwitchServerViewHolder(binding)
    }

    override fun getItemCount(): Int = serverList.size

    override fun getItemId(position: Int) = position.toLong()
    override fun getItemViewType(position: Int) = position

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: SwitchServerViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        val server = serverList[position]

        holder.binding.apply {
            tvServer.text = server.displayName

            if (position == lastSelectedPos) {
                ivServerIcon.isVisible = true
                cvServer.strokeColor = Color.parseColor(CacheUtil.getInstance().colorMain)
                tvServer.setTextColor(Color.parseColor("#171717"))

            }else {
                cvServer.strokeColor = ContextCompat.getColor(
                    holder.itemView.context,
                    android.R.color.transparent
                )
                tvServer.setTextColor(Color.parseColor("#000000"));
                ivServerIcon.isVisible = false
            }

            if(server.displayName.equals("custom", ignoreCase = true)){
                grpCustomUrl.visibility = View.VISIBLE
            }else {
                grpCustomUrl.visibility = View.GONE
            }

            teCustomUrl.setOnTouchListener { v, event ->
                if (MotionEvent.ACTION_UP == event.action) {
                    lastSelectedPos = holder.bindingAdapterPosition
                    callback.invoke(server)
                    notifyDataSetChanged()
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            teCustomUrl.requestFocus()
                        }, 1000
                    )
                    //
                }
                false
            }



            clServer.setOnClickListener {
                if(!server.displayName.equals("custom", ignoreCase = true)){
                    holder.itemView.context.cast<BaseActivity> {
                        BaseActivity.hideKeyboardFrom(it, teCustomUrl)
                    }
                }
                lastSelectedPos = holder.bindingAdapterPosition
                callback.invoke(server)
                notifyDataSetChanged()
            }
        }
    }




    inner class SwitchServerViewHolder(val binding : ItemSwitchServerBinding) : RecyclerView.ViewHolder(binding.root){

        init {
            binding.teCustomUrl.addTextChangedListener {
                customUrl = it.toString()
            }
        }

    }
    fun getCustomUrl() : String = customUrl
    interface SwitchServerListener{
        fun onClick(serverName : String)
    }

}