package com.fintrack.core

class LogEmailService : EmailService {
    private val log = logger<LogEmailService>()

    override suspend fun sendVerificationEmail(to: String, token: String) {
        val verificationLink = "http://localhost:8080/auth/verify-email-change?token=$token"
        log.info { "--- EMAIL SENT ---" }
        log.info { "To: $to" }
        log.info { "Subject: Verify your email change" }
        log.info { "Body: Please click the following link to verify your email change: $verificationLink" }
        log.info { "------------------" }
    }
}
