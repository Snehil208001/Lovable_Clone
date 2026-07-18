package com.snehil.auracode.core.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val DASHBOARD = "dashboard"
    const val BILLING = "billing"

    const val ARG_PROJECT_ID = "projectId"
    const val WORKSPACE = "workspace/{$ARG_PROJECT_ID}"

    fun workspace(projectId: Long) = "workspace/$projectId"
}
