package com.fintrack.feature.transaction.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DeleteResponse(
    val message: String,
    val cleared: Boolean
)