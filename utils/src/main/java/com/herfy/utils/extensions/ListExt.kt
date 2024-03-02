package com.herfy.utils.extensions

fun <T, U> List<T>.intersect(uList: List<U>, filterPredicate : (T, U) -> Boolean) = filter { m -> uList.any { filterPredicate(m, it)} }

fun <T, U> List<T>.contains(o: U, filterPredicate: (T, U) -> Boolean) = find { m -> any { filterPredicate(m, o) }}