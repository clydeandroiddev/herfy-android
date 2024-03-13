package com.herfy.faq

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.StorageStrategy
import com.herfy.common.utils.autoCleared
import com.herfy.faq.adapter.FrequencyAskAdapter
import com.herfy.faq.adapter.FrequencyAskQuestionStatusKeyProvider
import com.herfy.faq.adapter.FrequencyAskUIModel
import com.herfy.faq.adapter.FrequencyStatusLookUp
import com.herfy.faq.databinding.FragmentFrequentAskQuestionBinding
import com.herfy.utils.base.mvvm.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.launchWithTimber
import com.herfy.utils.helper.viewLifecycleScope
import com.herfy.utils.widgets.selectionTrackerProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class FrequencyAskQuestionFragment: BaseFragment<FrequencyAskQuestionViewModel, FragmentFrequentAskQuestionBinding>() {
    companion object {
        const val FREQUENCY_ASK_KEY = "FREQUENCY_ASK_KEY"
    }

    private var adapter: FrequencyAskAdapter by autoCleared()

    override val viewModel: FrequencyAskQuestionViewModel by viewModels()

    private val selectionTrackerProvider by selectionTrackerProvider()

    override val layout: Int = R.layout.fragment_frequent_ask_question

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setUpViews()
        binding.setUpAdapter()
        binding.setUpTranslations()
        setUpVmObservers()
    }
    private fun  FragmentFrequentAskQuestionBinding.setUpViews()  {
        icClose.ninjaTap(viewLifecycleScope) {
            findNavController().popBackStack()
        }
    }

    private fun FragmentFrequentAskQuestionBinding.setUpTranslations() {
        binding.txtFAQs.text = CacheUtil.getInstance().getTranslations(Text.FAQS_LABEL,"FAQs")
    }

    private fun setUpVmObservers() {
        viewLifecycleScope.launchWithTimber {
            viewModel
                .frequencyAskQuestionState
                .flowWithLifecycle(lifecycle = lifecycle, Lifecycle.State.CREATED)
                .collectLatest {
                    adapter.submitList(it)
                }
        }
    }

    private fun FragmentFrequentAskQuestionBinding.setUpAdapter() {
        adapter = FrequencyAskAdapter(::handleFrequencyClicked)
        frequencyRecycleView.adapter = adapter
        val selectionTracker = selectionTrackerProvider.createSelectionTracker(
            binding.frequencyRecycleView,
            FrequencyAskQuestionStatusKeyProvider(adapter),
            detailsLookup = FrequencyStatusLookUp(binding.frequencyRecycleView),
            selectionTrackerKey = FREQUENCY_ASK_KEY,
            storageStrategy = StorageStrategy.createParcelableStorage(FrequencyAskUIModel::class.java),
            selectionPredicate = SelectionPredicates.createSelectSingleAnything()
        )
        adapter.tracker = selectionTracker

    }

    private fun handleFrequencyClicked(frequencyAskUIModel: FrequencyAskUIModel) {
       adapter.tracker?.select(frequencyAskUIModel)
    }

}