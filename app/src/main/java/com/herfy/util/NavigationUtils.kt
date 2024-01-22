package com.herfy.util

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavGraph
import androidx.navigation.fragment.FragmentNavigator


object NavigationUtils {
    /**
     * This function will check navigation safety before starting navigation using direction
     *
     * @param navController NavController instance
     * @param direction     navigation operation
     */
    fun navigateSafe(navController: NavController?, direction: NavDirections) {
        navController?.let {
            val currentDestination = it.currentDestination
            if (currentDestination != null) {
                val navAction = currentDestination.getAction(direction.actionId)
                if (navAction != null) {
                    val destinationId = orEmpty(navAction.destinationId)
                    val currentNode: NavGraph? = if (currentDestination is NavGraph) currentDestination else currentDestination.parent
                    if (destinationId != 0 && currentNode != null && currentNode.findNode(destinationId) != null) {
                        it.navigate(direction)
                    }
                }
            }
        }
    }

    /**
     * This function will check navigation safety before starting navigation using resId and args bundle
     *
     * @param navController NavController instance
     * @param resId         destination resource id
     * @param args          bundle args
     */
    fun navigateSafe(navController: NavController?, @IdRes resId: Int, args: Bundle?) {
        navController?.let {
            val currentDestination = it.currentDestination
            if (currentDestination != null) {
                val navAction = currentDestination.getAction(resId)
                if (navAction != null) {
                    val destinationId = orEmpty(navAction.destinationId)
                    val currentNode: NavGraph? = if (currentDestination is NavGraph) currentDestination else currentDestination.parent
                    if (destinationId != 0 && currentNode != null && currentNode.findNode(destinationId) != null) {
                        it.navigate(resId, args)
                    }
                }
            }
        }

    }

    fun navigateSafe(navController: NavController?, direction: NavDirections, extras: FragmentNavigator.Extras) {
        navController?.let {
            val currentDestination = it.currentDestination
            if (currentDestination != null) {
                val navAction = currentDestination.getAction(direction.actionId)
                if (navAction != null) {
                    val destinationId = orEmpty(navAction.destinationId)
                    val currentNode: NavGraph? = if (currentDestination is NavGraph) currentDestination else currentDestination.parent
                    if (destinationId != 0 && currentNode != null && currentNode.findNode(destinationId) != null) {
                        it.navigate(direction, extras)
                    }
                }
            }
        }
    }

    private fun orEmpty(value: Int?): Int {
        return value ?: 0
    }
}