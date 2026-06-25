package com.example.walkhomesafe.api

import kotlinx.serialization.Serializable

/**
 * Request DTO for creating or updating a safety report.
 *
 * @property title Title of the report
 * @property description Description text of the report
 * @property latitude Latitude of the reported location
 * @property longitude Longitude of the reported location
 * @property ratingCategories Optional list of category ratings
 */
@Serializable
data class SaveReportDto(
    val title: String = "",
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val ratingCategories: List<ReportRatingDto>? = null
)

/**
 * Request DTO for voting on a report.
 *
 * @property reportId ID of the report to vote on
 * @property isUpvote true = upvote, false = downvote, null = remove vote
 */
@Serializable
data class SaveReportVoteDto(
    val reportId: Int,
    val isUpvote: Boolean? = null
)

/**
 * Response DTO for a single safety report from the API.
 *
 * @property id Unique report identifier
 * @property userName Display name of the author
 * @property title Report title
 * @property description Report description
 * @property latitude Latitude of the reported location
 * @property longitude Longitude of the reported location
 * @property createdAt ISO timestamp of creation
 * @property updatedAt ISO timestamp of last update, null if never updated
 * @property ratingCategories Optional list of category ratings
 * @property upvoteCount Number of upvotes
 * @property downvoteCount Number of downvotes
 */
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

/**
 * DTO for a rating within a single category.
 *
 * @property id Category identifier
 * @property name Display name of the category
 * @property rating Star rating value (e.g., 1-5)
 */
@Serializable
data class ReportRatingDto(
    val id: Int = 0,
    val name: String = "",
    val rating: Int = 0
)

/**
 * Response DTO for a user's vote on a report.
 *
 * @property reportId ID of the voted report
 * @property isUpvote true = upvote, false = downvote
 */
@Serializable
data class ReportVoteDto(
    val reportId: Int,
    val isUpvote: Boolean
)

/**
 * DTO for a report category from the API.
 *
 * @property id Unique category identifier
 * @property name Display name of the category
 */
@Serializable
data class ReportCategoryDto(
    val id: Int,
    val name: String = ""
)