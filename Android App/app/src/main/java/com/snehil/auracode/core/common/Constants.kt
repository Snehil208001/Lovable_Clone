package com.snehil.auracode.core.common

object Constants {
    const val DATASTORE_NAME = "auracode_prefs"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_EMAIL = "user_email"

    // Endpoints that must not carry the Authorization header.
    val PUBLIC_PATHS = listOf("/api/auth/login", "/api/auth/signup")
}
