package com.capstone.Algan

// 초대 코드
data class InvitationCode(
    val code: String, //코드
    var isUsed: Boolean, //사용가능여부
    val expiryDate: Long // 만료일
)
