package io.jans.chip.viewmodel.state

data class MainState (
    val opConfigurationPresent: Boolean = false,
    val fidoConfigurationPresent: Boolean = false,
    val attestationOptionSuccess: Boolean = false,
    val attestationResultSuccess: Boolean = false,
    val isClientRegistered: Boolean = false,
    val isUserIsAuthenticated: Boolean = false,
    val assertionOptionSuccess: Boolean = false,
    val assertionResultSuccess: Boolean = false,
    val errorInLoading: Boolean = false,
    val loadingErrorMessage: String = "",
    val isLoading: Boolean = false
)