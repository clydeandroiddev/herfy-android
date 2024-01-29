package com.herfy.utils.extensions

fun <T> T?.notNull(f: (it: T) -> Unit) {
    if (this != null) f(this)
}

inline fun <reified T> Any.cast(crossinline block: (T) -> Unit) {
    (this as? T).notNull {
        block(it)
    }
}