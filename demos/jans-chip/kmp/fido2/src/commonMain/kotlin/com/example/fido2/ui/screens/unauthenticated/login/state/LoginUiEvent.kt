package com.example.fido2.ui.screens.unauthenticated.login.state

/**
 * Login Screen Events
 */
sealed class LoginUiEvent {
    object Submit : LoginUiEvent()
    object Logout : LoginUiEvent()
}