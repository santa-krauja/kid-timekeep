package lv.zarin.timekeep.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.control.AllowAllControlPolicy
import lv.zarin.timekeep.domain.control.Control
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.FavouriteLookRepository
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.MAX_DURATION_MS
import lv.zarin.timekeep.domain.timer.MIN_DURATION_MS
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.isValidDuration
import lv.zarin.timekeep.domain.timer.newId

const val MAX_NAME_LENGTH = 40
const val DEFAULT_DURATION_MS = 5 * 60_000L

data class EditState(
    val name: String,
    val durationMs: Long,
    val look: Look,
    val saveAsPreset: Boolean,
    val keepLook: Boolean,
    val isEditingPreset: Boolean,
    val favourites: List<FavouriteLook>,
    val nameError: Boolean,
    val durationError: Boolean,
    val canEdit: Boolean = true,
    val canDelete: Boolean = true,
)

/** New timer (presetId == null) or Edit preset. */
class EditTimerViewModel(
    private val presetId: String?,
    private val presets: PresetRepository,
    private val favourites: FavouriteLookRepository,
    private val service: TimerService,
    private val lookPicker: LookPicker,
    private val clock: Clock,
    private val policy: ControlPolicy = AllowAllControlPolicy,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditState(
            name = "",
            durationMs = DEFAULT_DURATION_MS,
            look = PLACEHOLDER_LOOK,
            saveAsPreset = presetId == null,
            keepLook = false,
            isEditingPreset = presetId != null,
            favourites = emptyList(),
            nameError = false,
            durationError = false,
            canEdit = policy.isAllowed(Control.EDIT),
            canDelete = policy.isAllowed(Control.DELETE),
        ),
    )
    val state: StateFlow<EditState> = _state.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** The preset being edited, once loaded. */
    private var preset: Preset? = null

    init {
        viewModelScope.launch {
            val p = presetId?.let { presets.get(it) }
            preset = p
            val look = p?.pinnedLook ?: service.suggestLook()
            _state.update { s ->
                if (p == null) s.copy(look = look)
                else s.copy(name = p.name, durationMs = p.durationMs, look = look, keepLook = p.pinnedLook != null)
            }
        }
        viewModelScope.launch {
            favourites.observeAll().collect { list -> _state.update { it.copy(favourites = list) } }
        }
    }

    /** Input longer than [MAX_NAME_LENGTH] is ignored. */
    fun setName(name: String) {
        if (name.length > MAX_NAME_LENGTH) return
        _state.update { it.copy(name = name, nameError = false) }
    }

    /** Clamped to [MIN_DURATION_MS, MAX_DURATION_MS]. */
    fun setDuration(ms: Long) {
        val clamped = ms.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        _state.update { it.copy(durationMs = clamped, durationError = false) }
    }

    fun shuffle() {
        _state.update { it.copy(look = lookPicker.pick(setOf(it.look))) }
    }

    fun setLook(look: Look) {
        _state.update { it.copy(look = look) }
    }

    fun saveLookAsFavourite() {
        val look = _state.value.look
        viewModelScope.launch { favourites.add(look, clock.nowMs()) }
    }

    fun deleteFavourite(id: String) {
        if (!policy.isAllowed(Control.DELETE)) return
        viewModelScope.launch { favourites.delete(id) }
    }

    fun setSaveAsPreset(on: Boolean) {
        _state.update { it.copy(saveAsPreset = on) }
    }

    fun setKeepLook(on: Boolean) {
        _state.update { it.copy(keepLook = on) }
    }

    /** Starts a run (saving a preset first if asked). Returns the new timer id, or null when invalid. */
    suspend fun start(): String? = exclusively(whenBusy = null) {
        val s = validated() ?: return@exclusively null
        val name = s.name.trim()
        val savedPresetId = if (s.saveAsPreset) {
            val now = clock.nowMs()
            val order = (presets.observeAll().first().maxOfOrNull { it.sortOrder } ?: -1) + 1
            val p = Preset(
                id = newId(),
                name = name,
                durationMs = s.durationMs,
                pinnedLook = s.look.takeIf { s.keepLook },
                lastLook = s.look,
                sortOrder = order,
                createdAtMs = now,
                updatedAtMs = now,
            )
            presets.upsert(p)
            p.id
        } else {
            null
        }
        service.startOneOff(name, s.durationMs, s.look, savedPresetId).id
    }

    /** Saves the edited preset. Returns false when editing is not allowed, the input is invalid or the preset is gone. */
    suspend fun savePreset(): Boolean = exclusively(whenBusy = false) {
        if (!policy.isAllowed(Control.EDIT)) return@exclusively false
        val s = validated() ?: return@exclusively false
        val p = preset ?: presetId?.let { presets.get(it) } ?: return@exclusively false
        presets.upsert(
            p.copy(
                name = s.name.trim(),
                durationMs = s.durationMs,
                pinnedLook = s.look.takeIf { s.keepLook },
                updatedAtMs = clock.nowMs(),
            ),
        )
        true
    }

    suspend fun deletePreset(): Boolean = exclusively(whenBusy = false) {
        if (!policy.isAllowed(Control.DELETE)) return@exclusively false
        presetId?.let { presets.delete(it) }
        true
    }

    private suspend fun <T> exclusively(whenBusy: T, action: suspend () -> T): T {
        if (!_isSaving.compareAndSet(expect = false, update = true)) return whenBusy
        return try {
            action()
        } finally {
            _isSaving.value = false
        }
    }

    private fun validated(): EditState? {
        val s = _state.value
        val nameOk = s.name.trim().length in 1..MAX_NAME_LENGTH
        val durationOk = isValidDuration(s.durationMs)
        if (!nameOk || !durationOk) {
            _state.update { it.copy(nameError = !nameOk, durationError = !durationOk) }
            return null
        }
        return s
    }

    private companion object {
        /** Shown only until the first suggestion arrives. */
        val PLACEHOLDER_LOOK = Look(PictureId.HEART, SandColor.LAVENDER, PictureId.STAR, SandColor.SKY)
    }
}
