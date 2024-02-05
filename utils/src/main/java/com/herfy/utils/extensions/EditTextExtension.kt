package com.herfy.utils.extensions

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import reactivecircus.flowbinding.android.view.keys
import reactivecircus.flowbinding.android.widget.afterTextChanges
import reactivecircus.flowbinding.android.widget.editorActionEvents
import reactivecircus.flowbinding.android.widget.textChangeEvents
import reactivecircus.flowbinding.android.widget.textChanges
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

const val TEXT_WATCHER_DEBOUNCE_TIME = 400L


fun EditText.debounceTextChanges(): Flow<String> {
    return textChanges()
        .skipInitialValue()
        .debounce(TEXT_WATCHER_DEBOUNCE_TIME.milliseconds)
        .map { it.toString() }
}

fun EditText.isTextChangeEventNotEmpty(): Flow<Boolean> {
    return textChangeEvents()
        .skipInitialValue()
        .map { it.text }
        .map { it.isNotEmpty() }
}

fun EditText.unifiedInputs(
    editorAction: Int = EditorInfo.IME_ACTION_SEARCH,
    debounceDuration: Duration = TEXT_WATCHER_DEBOUNCE_TIME.milliseconds
): Flow<CharSequence> {
    return merge(
        editorActionEvents { it.actionId == editorAction }.map { text!! },
        keys { event ->
            event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER
        }.map { text!! },
        textChanges().skipInitialValue()
    ).debounce(debounceDuration)
}

fun EditText.debounceAfterTextChanges(
    debounceDuration: Duration = TEXT_WATCHER_DEBOUNCE_TIME.milliseconds
): Flow<CharSequence> {
    return afterTextChanges()
        .skipInitialValue()
        .map { it.editable }
        .map { it.toString() }
        .debounce(debounceDuration)
        .distinctUntilChanged()
}

fun EditText.debounceTextChanged(
    debounceDuration: Duration = TEXT_WATCHER_DEBOUNCE_TIME.milliseconds
): Flow<CharSequence> {
    return textChanges()
        .skipInitialValue()
        .map { it.toString() }
        .debounce(debounceDuration)
        .distinctUntilChanged()
}