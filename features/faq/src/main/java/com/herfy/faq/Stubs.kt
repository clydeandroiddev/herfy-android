package com.herfy.faq

import com.herfy.faq.adapter.FrequencyAskUIModel

object Stubs {
    fun stubsFrequencyAskQuestions(): List<FrequencyAskUIModel> {
        return listOf(FrequencyAskUIModel(frequencyTitle = "FAQ 1", id = 1, "https://www.google.com"),
            FrequencyAskUIModel(frequencyTitle = "FAQ 2 ", id = 2,"https://www.google.com"),
            FrequencyAskUIModel(frequencyTitle = "FAQ 3", id = 3,
                "https://www.google.com"))
    }
}