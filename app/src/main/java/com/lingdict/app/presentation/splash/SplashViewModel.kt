package com.lingdict.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingdict.app.data.local.importer.DictionaryImporter
import com.lingdict.app.data.local.importer.ImportProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashState {
    object Checking : SplashState()
    data class Importing(val progress: ImportProgress) : SplashState()
    object Completed : SplashState()
    data class Error(val message: String) : SplashState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dictionaryImporter: DictionaryImporter
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Checking)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    fun checkAndImport(context: android.content.Context) {
        viewModelScope.launch {
            try {
                // 检查是否已导入
                val isImported = dictionaryImporter.isImported()

                if (isImported) {
                    // 已导入，直接进入主界面
                    _state.value = SplashState.Completed
                } else {
                    // 开始导入
                    dictionaryImporter.importFromAssets(context, limit = 50000)
                        .catch { e ->
                            _state.value = SplashState.Error(e.message ?: "导入失败")
                        }
                        .collect { progress ->
                            _state.value = SplashState.Importing(progress)

                            // 导入完成
                            if (progress.current >= progress.total) {
                                _state.value = SplashState.Completed
                            }
                        }
                }
            } catch (e: Exception) {
                _state.value = SplashState.Error(e.message ?: "未知错误")
            }
        }
    }

    fun retry(context: android.content.Context) {
        _state.value = SplashState.Checking
        checkAndImport(context)
    }
}
