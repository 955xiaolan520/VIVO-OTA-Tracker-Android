package com.mytiantian.updater.vivo

data class VivoOtaResult(
    val updateVersion: String = "",
    val filename: String = "",
    val fileSizeBytes: String = "",
    val fileSizeMb: String = "",
    val downloadUrl: String = "",
    val changelogUrl: String = "",
    val rawResponse: String = ""
)

data class QueryHistoryEntry(
    val timestamp: Long,
    val model: String,
    val codename: String,
    val swVersion: String,
    val resultVersion: String,
    val fileSize: String,
    val downloadUrl: String
)

data class VivoOtaUiState(
    val selectedSeries: String = "",
    val selectedModelIndex: Int = 0,
    val selectedModel: String = "",
    val selectedCodename: String = "",
    val selectedModelSwVer: String = "",
    val deviceType: String = "phone",
    val softwareVersion: String = "",
    val androidVersion: Int = 15,
    val sn: String = "A0000000000000A",
    val isFullPackage: Boolean = true,
    val isLoading: Boolean = false,
    val result: VivoOtaResult? = null,
    val error: String? = null,
    val cryptoReady: Boolean = false,
    val manualMode: Boolean = false,
    val manualCodename: String = "",
    val manualModelSwVer: String = "",
    val manualModelName: String = "",
    val history: List<QueryHistoryEntry> = emptyList(),
    val toastMessage: String? = null,
    val changelogContent: String? = null
)
