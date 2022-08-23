package ru.tech.cookhelper.presentation.app.viewModel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.tech.cookhelper.domain.use_case.get_settings_list.GetSettingsListUseCase
import ru.tech.cookhelper.domain.use_case.get_user.GetUserUseCase
import ru.tech.cookhelper.presentation.app.components.UserState
import ru.tech.cookhelper.presentation.settings.components.ColorScheme
import ru.tech.cookhelper.presentation.settings.components.NightMode
import ru.tech.cookhelper.presentation.settings.components.Settings
import ru.tech.cookhelper.presentation.settings.components.SettingsState
import ru.tech.cookhelper.presentation.ui.utils.compose.UIText
import ru.tech.cookhelper.presentation.ui.utils.event.Event
import ru.tech.cookhelper.presentation.ui.utils.event.ViewModelEvents
import ru.tech.cookhelper.presentation.ui.utils.event.ViewModelEventsImpl
import ru.tech.cookhelper.presentation.ui.utils.navigation.Screen
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getSettingsListUseCase: GetSettingsListUseCase,
    getUserUseCase: GetUserUseCase
) : ViewModel(), ViewModelEvents<Event> by ViewModelEventsImpl() {

    private val _title: MutableState<UIText> = mutableStateOf(Screen.Home.Recipes.title)
    val title: UIText by _title

    private val _settingsState: MutableState<SettingsState> = mutableStateOf(SettingsState())
    val settingsState: SettingsState by _settingsState

    private val _userState: MutableState<UserState> = mutableStateOf(UserState())
    val userState: UserState by _userState

    init {
        getSettingsListUseCase().onEach { list ->
            var state = SettingsState()
            list.forEach { setting ->
                when (setting.id) {
                    Settings.DYNAMIC_COLORS.ordinal -> {
                        state = state.copy(
                            dynamicColors = setting.option.toBooleanStrictOrNull() ?: false
                        )
                    }
                    Settings.COLOR_SCHEME.ordinal -> {
                        val index = setting.option.toIntOrNull() ?: ColorScheme.BLUE.ordinal
                        state = state.copy(colorScheme = enumValues<ColorScheme>()[index])
                    }
                    Settings.NIGHT_MODE.ordinal -> {
                        val index = setting.option.toIntOrNull() ?: NightMode.SYSTEM.ordinal
                        state = state.copy(nightMode = enumValues<NightMode>()[index])
                    }
                    Settings.CART_CONNECTION.ordinal -> {
                        state = state.copy(
                            cartConnection = setting.option.toBoolean()
                        )
                    }
                }
            }
            _settingsState.value = state
        }.launchIn(viewModelScope)

        getUserUseCase().onEach { user ->
            if (user == null) sendEvent(Event.NavigateTo(Screen.Authentication.Login))
            else {
                sendEvent(
                    Event.NavigateIf(
                        predicate = { it is Screen.Authentication },
                        screen = Screen.Home.None
                    )
                )
                _userState.value = UserState(user, user.token)
            }
        }.launchIn(viewModelScope)
    }

    fun updateTitle(newTitle: UIText) {
        _title.value = newTitle
    }
}

fun String.toBoolean(): Boolean = this.toBooleanStrictOrNull() ?: false