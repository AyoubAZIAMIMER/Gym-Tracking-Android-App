// Purpose: First-run onboarding state — step 1 profile questions, step 2 program picker
// Inputs: WorkoutRepository (profile save, template previews, program creation/activation)
// Outputs: OnboardingUiState via StateFlow; onDone() once a program is active
package com.gymtracker.ui.screens.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.ProgramTemplates
import com.gymtracker.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,                 // 0 = profile, 1 = picker
    val previews: List<WorkoutRepository.TemplatePreview> = emptyList(),
    val selectedIndex: Int = 0,
    val saving: Boolean = false,
)

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository.get(app)

    private val _ui = MutableStateFlow(OnboardingUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val previews = ProgramTemplates.all.map { repo.previewTemplate(it) }
            _ui.update { it.copy(previews = previews) }
        }
    }

    fun saveProfileAndAdvance(name: String, weightKg: Double?, heightCm: Int?, weeklyGoal: Int) {
        repo.saveProfile(name, weightKg, heightCm, weeklyGoal)
        _ui.update { it.copy(step = 1) }
    }

    fun selectTemplate(index: Int) = _ui.update { it.copy(selectedIndex = index) }

    fun back() = _ui.update { it.copy(step = 0) }

    fun finish(onDone: () -> Unit) {
        val preview = _ui.value.previews.getOrNull(_ui.value.selectedIndex) ?: return onDone()
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            val programId = repo.createFromTemplate(preview.template)
            repo.setActiveProgram(programId)
            onDone()
        }
    }
}
