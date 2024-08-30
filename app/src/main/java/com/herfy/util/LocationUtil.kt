package com.herfy.util

import com.herfy.utils.constants.Values.INITIAL_SIZE
import com.herfy.utils.constants.Values.LOCATION_LIMIT
import com.herfy.utils.constants.Values.SECOND_PAGE

object LocationUtil {

    fun locationOffset(page: Int): String {
        var offset = ((page - SECOND_PAGE) * LOCATION_LIMIT.toInt()) + INITIAL_SIZE.toInt()
        if (page == SECOND_PAGE) {
            offset = INITIAL_SIZE.toInt()
        }
        return offset.toString()
    }

}