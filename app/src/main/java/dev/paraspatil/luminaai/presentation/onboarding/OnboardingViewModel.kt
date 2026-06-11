package dev.paraspatil.luminaai.presentation.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.paraspatil.luminaai.data.local.ProfileDataStore
import dev.paraspatil.luminaai.data.local.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val age: String = "",
    val phone: String = "",
    val otp: String = "",
    val selectedTraits: List<String> = emptyList(),
    val errorMessage: String? = null
)

class OnboardingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dataStore = ProfileDataStore(application)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val availableTraits = listOf(
        "Friendly", "Professional", "Witty", "Empathetic",
        "Direct", "Creative", "Logical", "Energetic", "Calm"
    )

    fun updateName(name: String) = _uiState.update { it.copy(name = name, errorMessage = null) }
    fun updateAge(age: String) = _uiState.update { it.copy(age = age, errorMessage = null) }
    fun updatePhone(phone: String) = _uiState.update { it.copy(phone = phone, errorMessage = null) }
    fun updateOtp(otp: String) = _uiState.update { it.copy(otp = otp, errorMessage = null) }

    fun toggleTrait(trait: String) {
        _uiState.update { state ->
            val current = state.selectedTraits.toMutableList()
            if (current.contains(trait)) {
                current.remove(trait)
            } else {
                if (current.size < 3) current.add(trait)
            }
            state.copy(selectedTraits = current, errorMessage = null)
        }
    }

    fun validateStep2(): Boolean {
        val state = _uiState.value
        if (state.name.isBlank() || state.age.isBlank() || state.phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "All fields are required") }
            return false
        }
        if (state.phone.length != 10 || !state.phone.all { it.isDigit() }) {
            _uiState.update { it.copy(errorMessage = "Phone must be exactly 10 digits") }
            return false
        }
        if (state.otp != "1234") { // Mock OTP requirement
            _uiState.update { it.copy(errorMessage = "Invalid OTP. Use 1234.") }
            return false
        }
        return true
    }

    fun validateAndSave(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.selectedTraits.size != 3) {
            _uiState.update { it.copy(errorMessage = "Please select exactly 3 traits") }
            return
        }

        // Save to DataStore to persist across app restarts
        viewModelScope.launch {
            val profile = UserProfile(
                name = state.name,
                age = state.age,
                phone = state.phone,
                selectedTraits = state.selectedTraits
            )
            dataStore.saveProfile(profile)
            onComplete()
        }
    }
}