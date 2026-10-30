package cl.duoc.mpazpro.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.mpazpro.MPazApp
import cl.duoc.mpazpro.di.AppContainer

/**
 * Crea un ViewModel con acceso al contenedor de dependencias y al SavedStateHandle
 * (donde llegan los argumentos de navegación, ej. el id de la asignación).
 */
@Composable
inline fun <reified VM : ViewModel> mpazViewModel(
    crossinline crear: (AppContainer, SavedStateHandle) -> VM,
): VM = viewModel(
    factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as MPazApp
            crear(app.contenedor, createSavedStateHandle())
        }
    },
)
