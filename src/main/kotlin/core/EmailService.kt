package com.fintrack.core

interface EmailService {
    suspend fun sendVerificationEmail(to: String, token: String)
}
