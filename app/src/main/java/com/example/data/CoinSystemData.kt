package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoinSystemUser(
    val userId: String,
    val coins: Int,
    val diamonds: Int
)

@JsonClass(generateAdapter = true)
data class CoinSystemTransaction(
    val type: String, // PURCHASE, ADMIN_GENERATION, GIFT
    val user: String,
    val amount: Int,
    val receiver: String? = null,
    val diamonds: Int? = null,
    val date: String
)
