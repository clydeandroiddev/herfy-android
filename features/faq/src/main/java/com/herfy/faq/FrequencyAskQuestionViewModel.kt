package com.herfy.faq

import com.herfy.faq.adapter.FrequencyAskUIModel
import com.herfy.utils.base.mvvm.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FrequencyAskQuestionViewModel @Inject constructor(): BaseViewModel() {

    private val _frequencyAskQuestionState = MutableStateFlow<List<FrequencyAskUIModel>>(Stubs.stubsFrequencyAskQuestions())

    val frequencyAskQuestionState = _frequencyAskQuestionState.asStateFlow()
}