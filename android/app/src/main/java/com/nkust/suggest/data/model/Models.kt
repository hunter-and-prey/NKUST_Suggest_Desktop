package com.nkust.suggest.data.model

data class UnitItem(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val nameJa: String
)

data class KeyValueItem(
    val key: String,
    val labelZh: String,
    val labelEn: String,
    val labelJa: String
)

data class SuggestionPayload(
    val subject: String,
    val unitId: String,
    val content: String,
    val name: String,
    val guestType: String,
    val email: String,
    val phone: String,
    val secrecyType: String,
    val doneOpen: String
)

data class SubmitResult(
    val success: Boolean,
    val message: String,
    val confirmationUrl: String? = null
)

data class UserProfile(
    val name: String = "",
    val guestType: String = "1",
    val email: String = "",
    val phone: String = "",
    val secrecyType: String = "9",
    val doneOpen: String = "1",
    val mailPassword: String = ""
)
