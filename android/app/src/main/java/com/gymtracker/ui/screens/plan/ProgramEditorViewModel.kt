// Purpose: Program editor state — days and exercises of one program
// Inputs: programId from the nav route; WorkoutRepository program CRUD
// Outputs: ProgramEditorUiState via StateFlow
package com.gymtracker.ui.screens.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.screens.session.PickerItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgramEditorUiState(
    val detail: WorkoutRepository.ProgramDetail? = null,
    val isActive: Boolean = false,
    val pickerForDayId: String? = null,
    val pickerItems: List<PickerItem> = emptyList(),
    val deleted: Boolean = false,
    // non-null while a row's picker is open in "swap this one" mode, not "add a new one"
    val replacingExerciseId: String? = null,
    // non-null while the Edit-target sheet is open for this program-exercise row id
    val editingExerciseId: String? = null,
)

class ProgramEditorViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val repo = WorkoutRepository.get(app)
    private val programId: String = savedStateHandle["programId"] ?: ""

    private val _ui = MutableStateFlow(ProgramEditorUiState())
    val ui = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _ui.update {
            it.copy(
                detail = repo.programDetail(programId),
                isActive = repo.activeProgramId() == programId,
            )
        }
    }

    fun toggleActive() = viewModelScope.launch {
        repo.setActiveProgram(if (_ui.value.isActive) null else programId)
        refresh()
    }

    fun addDay() = viewModelScope.launch {
        val count = _ui.value.detail?.days?.size ?: 0
        repo.addDay(programId, "Day ${count + 1}")
        refresh()
    }

    fun deleteDay(dayId: String) = viewModelScope.launch {
        repo.deleteDay(dayId)
        refresh()
    }

    fun openPicker(dayId: String) = viewModelScope.launch {
        val items = repo.exercisesSnapshot().map { PickerItem(it.id, it.name, it.muscles) }
        _ui.update { it.copy(pickerForDayId = dayId, pickerItems = items) }
    }

    fun closePicker() = _ui.update { it.copy(pickerForDayId = null) }

    fun addExercise(item: PickerItem) = viewModelScope.launch {
        val dayId = _ui.value.pickerForDayId ?: return@launch
        val exerciseId = item.dbExerciseId ?: return@launch
        repo.addExerciseToDay(dayId, exerciseId)
        _ui.update { it.copy(pickerForDayId = null) }
        refresh()
    }

    fun removeExercise(programExerciseId: String) = viewModelScope.launch {
        repo.removeProgramExercise(programExerciseId)
        refresh()
    }

    fun toggleSuperset(programExerciseId: String) = viewModelScope.launch {
        val dayId = dayIdOf(programExerciseId) ?: return@launch
        repo.toggleProgramSuperset(dayId, programExerciseId)
        refresh()
    }

    fun moveExercise(programExerciseId: String, delta: Int) = viewModelScope.launch {
        val dayId = dayIdOf(programExerciseId) ?: return@launch
        repo.moveProgramExercise(dayId, programExerciseId, delta)
        refresh()
    }

    fun openEditTarget(programExerciseId: String) =
        _ui.update { it.copy(editingExerciseId = programExerciseId) }

    fun closeEditTarget() = _ui.update { it.copy(editingExerciseId = null) }

    fun saveEditTarget(sets: Int, repMin: Int, repMax: Int) = viewModelScope.launch {
        val id = _ui.value.editingExerciseId ?: return@launch
        repo.updateProgramExerciseTarget(id, sets, repMin, repMax)
        _ui.update { it.copy(editingExerciseId = null) }
        refresh()
    }

    fun openReplace(programExerciseId: String) = viewModelScope.launch {
        val items = repo.exercisesSnapshot().map { PickerItem(it.id, it.name, it.muscles) }
        _ui.update { it.copy(replacingExerciseId = programExerciseId, pickerItems = items) }
    }

    fun closeReplace() = _ui.update { it.copy(replacingExerciseId = null) }

    fun confirmReplace(item: PickerItem) = viewModelScope.launch {
        val id = _ui.value.replacingExerciseId ?: return@launch
        val newExerciseId = item.dbExerciseId ?: return@launch
        repo.replaceProgramExercise(id, newExerciseId)
        _ui.update { it.copy(replacingExerciseId = null) }
        refresh()
    }

    private fun dayIdOf(programExerciseId: String): String? =
        _ui.value.detail?.days
            ?.firstOrNull { day -> day.exercises.any { it.row.id == programExerciseId } }
            ?.day?.id

    fun deleteProgram() = viewModelScope.launch {
        repo.deleteProgram(programId)
        _ui.update { it.copy(deleted = true) }
    }
}
