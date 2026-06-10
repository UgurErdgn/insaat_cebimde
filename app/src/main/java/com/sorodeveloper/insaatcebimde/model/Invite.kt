package com.sorodeveloper.insaatcebimde.model

data class Invite(
    val inviteId: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val senderName: String = "",
    val insaatId: String = "",
    val insaatName: String = "",
    val locationPath: String = "",
    val jobPath: String = "",
    val canDelegate: Boolean = false,
    val status: String = "pending", // pending, accepted, rejected
    val createdAt: Long = 0L
)
