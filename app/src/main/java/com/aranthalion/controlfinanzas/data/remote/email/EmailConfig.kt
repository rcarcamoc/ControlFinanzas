package com.aranthalion.controlfinanzas.data.remote.email

import java.util.UUID

data class EmailConfig(
    val id: String = UUID.randomUUID().toString(),
    val host: String = "mail.recc.001webhospedaje.com",
    val port: Int = 993,
    val username: String = "recibemail@recc.001webhospedaje.com",
    val password: String = "Gatochuchu",
    val protocol: String = "imaps", // or "imap"
    val useSSL: Boolean = true,
    val enabled: Boolean = true
)
