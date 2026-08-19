// Purpose: Is any of our UI on screen right now?
//          The rest timer has two bubbles by design — the Compose one inside the session screen,
//          and the WindowManager overlay that follows you out of the app. Exactly one should be
//          visible at a time; without this they both showed while the session was open.
// Inputs: MainActivity's onStart/onStop
// Outputs: AppForeground.visible — collected by RestTimerService
package com.gymtracker.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppForeground {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun set(value: Boolean) {
        _visible.value = value
    }
}
