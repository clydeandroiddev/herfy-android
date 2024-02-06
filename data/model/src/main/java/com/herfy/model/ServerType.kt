package com.herfy.model


data class ServerType (
    var url : String,
    val displayName : String,
    val contentUrlPrefix : String,
    val isProd : Boolean
)