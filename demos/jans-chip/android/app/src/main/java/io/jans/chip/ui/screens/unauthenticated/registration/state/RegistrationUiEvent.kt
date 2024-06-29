package io.jans.chip.ui.screens.unauthenticated.registration.state

/**
 * Registration Screen Events
 */
sealed class RegistrationUiEvent {
    data class UsernameChanged(val inputValue: String) : RegistrationUiEvent()
    data class PasswordChanged(val inputValue: String) : RegistrationUiEvent()
    object Submit : RegistrationUiEvent()
    object Loading : RegistrationUiEvent()
}