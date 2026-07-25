package com.mytiantian.updater.vivo

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytiantian.updater.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VivoOtaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VivoOtaUiState())
    val uiState: StateFlow<VivoOtaUiState> = _uiState.asStateFlow()

    private val client: VivoOtaClient
    private val prefs: android.content.SharedPreferences

    init {
        val ctx = AndroidAppContext.getApplicationContext()
            ?: throw IllegalStateException("AndroidAppContext not initialized")
        prefs = ctx.getSharedPreferences("vivo_ota", Context.MODE_PRIVATE)
        VivoDeviceDatabase.load(ctx)
        client = VivoOtaClient(ctx)
        loadHistory()
        initCrypto()
    }

    private fun initCrypto() {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = try {
                client.initCrypto()
            } catch (e: Exception) {
                Log.e("VivoOtaViewModel", "Crypto init failed", e)
                false
            }
            _uiState.update { it.copy(cryptoReady = ok) }
        }
    }

    fun selectSeries(series: String) {
        val devices = VivoDeviceDatabase.devicesOf(series)
        val first = devices.firstOrNull()
        val detectedType = detectDeviceType(series)
        _uiState.update {
            it.copy(
                selectedSeries = series,
                selectedModelIndex = 0,
                selectedModel = first?.model ?: "",
                selectedCodename = first?.codename ?: "",
                selectedModelSwVer = first?.model_sw_ver ?: "",
                deviceType = detectedType
            )
        }
    }

    fun selectDevice(index: Int) {
        val devices = VivoDeviceDatabase.devicesOf(_uiState.value.selectedSeries)
        val device = devices.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                selectedModelIndex = index,
                selectedModel = device.model,
                selectedCodename = device.codename,
                selectedModelSwVer = device.model_sw_ver
            )
        }
    }

    fun updateSoftwareVersion(v: String) { _uiState.update { it.copy(softwareVersion = v) } }
    fun updateAndroidVersion(v: Int) { _uiState.update { it.copy(androidVersion = v) } }
    fun updateSn(v: String) { _uiState.update { it.copy(sn = v) } }
    fun updateDeviceType(type: String) { _uiState.update { it.copy(deviceType = type) } }
    fun togglePackageType() { _uiState.update { it.copy(isFullPackage = !it.isFullPackage) } }
    fun toggleManualMode() { _uiState.update { it.copy(manualMode = !it.manualMode) } }
    fun updateManualCodename(v: String) { _uiState.update { it.copy(manualCodename = v) } }
    fun updateManualModelSwVer(v: String) { _uiState.update { it.copy(manualModelSwVer = v) } }
    fun updateManualModelName(v: String) { _uiState.update { it.copy(manualModelName = v) } }
    fun clearToast() { _uiState.update { it.copy(toastMessage = null) } }

    private fun fetchChangelog(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = client.fetchChangelog(url)
            _uiState.update { it.copy(changelogContent = content ?: "暂无更新日志") }
        }
    }

    private fun detectDeviceType(series: String): String {
        return if (series.contains("平板") || series.contains("穿戴")) "tablet" else "phone"
    }

    fun query() {
        val state = _uiState.value
        if (!state.cryptoReady) return

        val codename: String
        val modelSwVer: String
        val modelName: String

        if (state.manualMode) {
            codename = state.manualCodename.trim()
            modelSwVer = state.manualModelSwVer.trim()
            modelName = state.manualModelName.trim().ifEmpty { codename }
            if (codename.isEmpty()) return
        } else {
            codename = state.selectedCodename
            modelSwVer = state.selectedModelSwVer
            modelName = state.selectedModel
            if (codename.isEmpty()) return
        }
        if (state.softwareVersion.isEmpty()) return

        _uiState.update { it.copy(isLoading = true, error = null, result = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = client.query(
                    codename = codename,
                    modelSwVer = modelSwVer,
                    swVersion = state.softwareVersion,
                    androidVersion = state.androidVersion,
                    isPhone = state.deviceType == "phone",
                    isFull = state.isFullPackage,
                    sn = state.sn
                )

                val hasUpdate = result.updateVersion.isNotEmpty() &&
                        result.updateVersion != "(Not found)" &&
                        result.filename.isNotEmpty() &&
                        result.filename != "(Not found)"

                if (hasUpdate) {
                    addToHistory(modelName, codename, state.softwareVersion, result)
                    _uiState.update {
                        it.copy(isLoading = false, result = result, toastMessage = "查询成功")
                    }
                    if (result.changelogUrl.isNotEmpty() && result.changelogUrl != "(Not found)") {
                        _uiState.update { it.copy(changelogContent = "loading") }
                        fetchChangelog(result.changelogUrl)
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, result = result, toastMessage = "未查询到更新")
                    }
                }
            } catch (e: Exception) {
                Log.e("VivoOtaViewModel", "Query failed", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun addToHistory(
        model: String,
        codename: String,
        swVersion: String,
        result: VivoOtaResult
    ) {
        val entry = QueryHistoryEntry(
            timestamp = System.currentTimeMillis(),
            model = model,
            codename = codename,
            swVersion = swVersion,
            resultVersion = result.updateVersion,
            fileSize = result.fileSizeMb,
            downloadUrl = result.downloadUrl
        )
        val updated = (listOf(entry) + _uiState.value.history).take(20)
        _uiState.update { it.copy(history = updated) }
        saveHistory(updated)
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
        saveHistory(emptyList())
    }

    private fun loadHistory() {
        try {
            val json = prefs.getString("history", "") ?: ""
            if (json.isEmpty()) return
            val arr = JSONArray(json)
            val list = mutableListOf<QueryHistoryEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    QueryHistoryEntry(
                        timestamp = o.getLong("timestamp"),
                        model = o.getString("model"),
                        codename = o.getString("codename"),
                        swVersion = o.getString("swVersion"),
                        resultVersion = o.optString("resultVersion", ""),
                        fileSize = o.optString("fileSize", ""),
                        downloadUrl = o.optString("downloadUrl", "")
                    )
                )
            }
            _uiState.update { it.copy(history = list) }
        } catch (e: Exception) {
            Log.w("VivoOtaViewModel", "Failed to load history", e)
        }
    }

    private fun saveHistory(list: List<QueryHistoryEntry>) {
        try {
            val arr = JSONArray()
            for (e in list) {
                arr.put(JSONObject().apply {
                    put("timestamp", e.timestamp)
                    put("model", e.model)
                    put("codename", e.codename)
                    put("swVersion", e.swVersion)
                    put("resultVersion", e.resultVersion)
                    put("fileSize", e.fileSize)
                    put("downloadUrl", e.downloadUrl)
                })
            }
            prefs.edit().putString("history", arr.toString()).apply()
        } catch (e: Exception) {
            Log.w("VivoOtaViewModel", "Failed to save history", e)
        }
    }

    fun formatTime(ts: Long): String {
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
