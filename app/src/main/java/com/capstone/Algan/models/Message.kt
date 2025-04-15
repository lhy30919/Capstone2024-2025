package com.capstone.Algan.models

data class Message(
    val content: String,
    val timestamp: String,  // "yyyy-MM-dd HH:mm:ss" 형식
    val username: String,
    val profileImageUrl: String,
    val imageUri: String? = null,
    val companyCode: String
) {
    constructor() : this("", "", "", "", null, "")
}
