package com.example.walkhomesafe.api

import kotlinx.serialization.Serializable

@Serializable
data class SaveReportDto(
    val title: String = "",
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val ratingCategories: List<ReportRatingDto>? = null
)

@Serializable
data class SaveReportVoteDto(
    val reportId: Int,
    val isUpvote: Boolean? = null // set null to delete vote
)

@Serializable
data class ReportDto(
    val id: Int,
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val createdAt: String = "",
    val updatedAt: String? = null,
    val ratingCategories: List<ReportRatingDto>? = null,
    val upvoteCount: Int = 0,
    val downvoteCount: Int = 0
)

@Serializable
data class ReportRatingDto(
    val id: Int = 0,
    val name: String = "",
    val rating: Int = 0
)

@Serializable
data class ReportCategoryDto(
    val id: Int,
    val name: String = ""
)