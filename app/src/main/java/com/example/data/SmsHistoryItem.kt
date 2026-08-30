package com.example.data

data class SmsHistoryItem(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val fullMessage: String,
    val timestamp: Long,
    val isDirectSms: Boolean,
    val isSuccess: Boolean
)
