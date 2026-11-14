package com.censustracker.logging

import kotlinx.serialization.Serializable

@Serializable
data class ErrorLogEntry(
    val id: Long,
    val timestamp: Long,
    val action: String,
    val message: String,
    val detail: String? = null
)
