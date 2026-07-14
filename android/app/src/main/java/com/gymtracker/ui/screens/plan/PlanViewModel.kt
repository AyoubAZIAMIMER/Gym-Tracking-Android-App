// Purpose: Plan tab state — programs list, active program's next day, prebuilt templates
// Inputs: WorkoutRepository programs + ProgramTemplates
// Outputs: PlanUiState via StateFlow
package com.gymtracker.ui.screens.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.ProgramTemplates
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.data.db.ProgramEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanUiState(
    val programs: List<ProgramEntity> = emptyList(),
    val activeProgramId: String? = null,
    val next: WorkoutRepository.NextProgramDay? = null,
    val templates: List<ProgramTemplates.Template> = ProgramTemplates.all,
)

class PlanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val _ui = MutableStateFlow(PlanUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observePrograms().collect { programs ->
                _ui.update {
                    it.copy(
                        programs = programs,
                        activeProgramId = repo.activeProgramId(),
                        next = repo.nextProgramDay(),
                    )
                }
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(activeProgramId = repo.activeProgramId(), next = repo.nextProgramDay()) }
    }

    fun createProgram(name: String, onCreated: (String) -> Unit) = viewModelScope.launch {
        val id = repo.createProgram(name.ifBlank { "My program" })
        onCreated(id)
    }

    fun addTemplate(template: ProgramTemplates.Template) = viewModelScope.launch {
        val id = repo.createFromTemplate(template)
        // first program added becomes active automatically
        if (repo.activeProgramId() == null) repo.setActiveProgram(id)
        refresh()
    }

    fun setActive(programId: String?) {
        repo.setActiveProgram(programId)
        refresh()
    }
}
