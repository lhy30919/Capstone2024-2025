package com.capstone.Algan

// 사용자
data class User(
    val email: String,
    val username: String,
    val password: String,
    val role: String,
    val phone: String,
    val companyName: String? = null
)

