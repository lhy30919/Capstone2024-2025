package com.capstone.Algan.models

data class Message(
    val content: String,
    val timestamp: String,
    val username: String,
    val profileImageUrl: String,
    val imageUri: String? = null,
    val companyCode: String // 회사 코드 추가
) {
    constructor() : this("", "", "", "", null, "")

    val timestampAsLong: Long
        get() = timestamp.toLongOrNull() ?: 0L
}
