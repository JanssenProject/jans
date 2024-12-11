package com.example.fido2.ui.screens.unauthenticated.registration

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.fido2.ui.common.state.ErrorState
import com.example.fido2.ui.screens.unauthenticated.registration.state.RegistrationErrorState
import com.example.fido2.ui.screens.unauthenticated.registration.state.RegistrationState
import com.example.fido2.ui.screens.unauthenticated.registration.state.RegistrationUiEvent
import com.example.fido2.ui.screens.unauthenticated.registration.state.emailEmptyErrorState
import com.example.fido2.ui.screens.unauthenticated.registration.state.passwordEmptyErrorState

class RegistrationViewModel : ViewModel() {

    var registrationState = mutableStateOf(RegistrationState())
        private set

    /**
     * Function called on any login event [RegistrationUiEvent]
     */
    fun onUiEvent(registrationUiEvent: RegistrationUiEvent): Boolean {
        when (registrationUiEvent) {

            // Email id changed event
            is RegistrationUiEvent.UsernameChanged -> {
                registrationState.value = registrationState.value.copy(
                    username = registrationUiEvent.inputValue,
                    errorState = registrationState.value.errorState.copy(
                        emailIdErrorState = if (registrationUiEvent.inputValue.trim().isEmpty()) {
                            // Email id empty state
                            emailEmptyErrorState
                        } else {
                            // Valid state
                            ErrorState()
                        }

                    )
                )
            }

            // Password changed event
            is RegistrationUiEvent.PasswordChanged -> {
                registrationState.value = registrationState.value.copy(
                    password = registrationUiEvent.inputValue,
                    errorState = registrationState.value.errorState.copy(
                        passwordErrorState = if (registrationUiEvent.inputValue.trim().isEmpty()) {
                            // Password Empty state
                            passwordEmptyErrorState
                        } else {
                            // Valid state
                            ErrorState()
                        }

                    )
                )
            }

            is RegistrationUiEvent.ValidateInputs -> {
                val inputsValidated = validateInputs()
                if (inputsValidated) {
                    // TODO Trigger registration in authentication flow
                    registrationState.value =
                        registrationState.value.copy(isValidationSuccessful = inputsValidated)
                } else {
                    return false
                }
            }

            // Submit Registration event
            is RegistrationUiEvent.Submit -> {
                // TODO Trigger registration in authentication flow
                registrationState.value = registrationState.value.copy(isRegistrationSuccessful = true)
            } else -> { return false}
        }
        return true
    }

    /**
     * Function to validate inputs
     * Ideally it should be on domain layer (usecase)
     * @return true -> inputs are valid
     * @return false -> inputs are invalid
     */
    private fun validateInputs(): Boolean {
        val username = registrationState.value.username.trim()
        val passwordString = registrationState.value.password.trim()

        return when {

            // Email empty
            username.isEmpty() -> {
                registrationState.value = registrationState.value.copy(
                    errorState = RegistrationErrorState(
                        emailIdErrorState = emailEmptyErrorState
                    )
                )
                false
            }

            //Password Empty
            passwordString.isEmpty() -> {
                registrationState.value = registrationState.value.copy(
                    errorState = RegistrationErrorState(
                        passwordErrorState = passwordEmptyErrorState
                    )
                )
                false
            }

            // No errors
            else -> {
                // Set default error state
                registrationState.value =
                    registrationState.value.copy(errorState = RegistrationErrorState())
                true
            }
        }
    }
}