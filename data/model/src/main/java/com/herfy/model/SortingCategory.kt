package com.herfy.model

import com.herfy.enum.HistorySortType

data class SortingCategory (
    val label : String,
    var isSelected : Boolean = false,
    val sortType : HistorySortType
)