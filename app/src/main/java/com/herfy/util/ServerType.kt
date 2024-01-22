package com.herfy.util

/*enum class ServerType {
    DEV, STAGING, PRODUCTION, TEST_CONSUMER, CUSTOM, NONE
}*/

data class ServerType (
    var url : String,
    val displayName : String,
    val contentUrlPrefix : String,
    val isProd : Boolean
)