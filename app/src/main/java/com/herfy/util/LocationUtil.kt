package com.herfy.util

import com.herfy.utils.constants.Values.INITIAL_SIZE
import com.herfy.utils.constants.Values.OOM_OFFSET
import com.herfy.utils.constants.Values.SECOND_PAGE

object LocationUtil {

    fun locationOffset(page: Int): Int {
        var offset = ((page - SECOND_PAGE) * OOM_OFFSET) + INITIAL_SIZE
        if (page == SECOND_PAGE) {
            offset = INITIAL_SIZE
        }
        return offset
    }

}