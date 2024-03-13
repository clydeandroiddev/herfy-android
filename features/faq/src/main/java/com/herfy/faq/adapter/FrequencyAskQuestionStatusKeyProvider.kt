package com.herfy.faq.adapter

import androidx.recyclerview.selection.ItemKeyProvider

class FrequencyAskQuestionStatusKeyProvider(private val adapter: FrequencyAskAdapter) :
    ItemKeyProvider<FrequencyAskUIModel>(SCOPE_CACHED) {

    override fun getKey(position: Int): FrequencyAskUIModel? =
        adapter.currentList[position]

    override fun getPosition(key: FrequencyAskUIModel): Int= adapter.currentList.indexOf(key)

    }
