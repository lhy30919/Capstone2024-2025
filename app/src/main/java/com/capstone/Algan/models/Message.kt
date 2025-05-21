package com.capstone.Algan.models

data class Message(
    val content: String,
    val timestamp: String,
    val username: String,
    val profileImageUrl: String,
    val imageUri: String? = null,
    val companyCode: String,
    val userId: String? = null
) {
    constructor() : this("", "", "", "", null, "", null)
}
