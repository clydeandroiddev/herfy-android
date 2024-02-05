package com.herfy.utils.extensions

import android.os.Environment
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.findNavController
import java.io.File
import java.io.IOException

fun Fragment.navigate(dest: NavDirections, navigatorExtras: Navigator.Extras? = null) {
    if (navigatorExtras != null) {
        findNavController()
            .navigate(
                dest,
                navigatorExtras
            )
    } else {
        findNavController().navigate(dest)
    }
}

fun Fragment.navigate(dest: NavDirections, navOptions: NavOptions) {
    findNavController()
        .navigate(
            dest,
            navOptions
        )
}

fun Fragment.navigateUp() = findNavController().navigateUp()

fun Fragment.isDestinationInBackstack(@IdRes destinationId: Int): Boolean {
    return try {
        findNavController().getBackStackEntry(destinationId)
        true
    } catch (e: Exception) {
        false
    }
}

@Throws(IOException::class)
fun Fragment.createImageFile(): File {
    val storageDir: File = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

    return File.createTempFile(
        "avatar",
        ".jpg",
        storageDir
    )
}

fun NavController.getCurrentBackStackEntrySavedStateHandle(): SavedStateHandle? {
    return this.currentBackStackEntry?.savedStateHandle
}

fun <T> Fragment.getNavigationResult(key: String, onResult: (result: T) -> Unit) {
    findNavController().currentBackStackEntry?.let { navBackStackEntry ->
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                navBackStackEntry.savedStateHandle.contains(key)
            ) {
                val result = navBackStackEntry.savedStateHandle.get<T>(key)
                result?.let(onResult)
                navBackStackEntry.savedStateHandle.remove<T>(key)
            }
        }
        navBackStackEntry.lifecycle.addObserver(observer)

        try {
            viewLifecycleOwner.lifecycle.addObserver(
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        navBackStackEntry.lifecycle.removeObserver(observer)
                    }
                }
            )
        } catch (e: Exception) {
            lifecycle.addObserver(
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        navBackStackEntry.lifecycle.removeObserver(observer)
                    }
                }
            )
        }
    }
}

fun <T> Fragment.setNavigationResult(result: T, key: String = "result") {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
}

fun NavController.safeNavigate(direction: NavDirections) {
    currentDestination?.getAction(direction.actionId)?.run {
        navigate(direction)
    }
}