package com.mytiantian.updater.vivo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mytiantian.updater.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import androidx.compose.ui.state.ToggleableState
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun VivoApp(viewModel: VivoOtaViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAbout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val darkMode = isSystemInDarkTheme()

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val codename = if (state.manualMode) state.manualCodename else state.selectedCodename

    val colorScheme = if (darkMode) {
        darkColorScheme().copy(
            background = Color(0xFF0F0F0F),
            surfaceContainer = Color(0xFF1E1E1E),
            secondaryContainer = Color(0xFF1E1E1E)
        )
    } else {
        lightColorScheme().copy(
            background = Color(0xFFF2F2F7),
            surfaceContainer = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFFFFF)
        )
    }
    MiuixTheme(colorScheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                SmallTopAppBar(
                    title = stringResource(R.string.app_name),
                    navigationIcon = {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher),
                            contentDescription = stringResource(R.string.app_name),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { showAbout = true }
                        )
                    },
                    scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background).imePadding(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { CryptoStatusCard(state.cryptoReady) }

                item { ManualModeCard(state, viewModel) }

                item {
                    AnimatedVisibility(
                        visible = state.manualMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ManualInputCard(state, viewModel)
                    }
                    AnimatedVisibility(
                        visible = !state.manualMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SeriesDropdownCard(state, viewModel)
                            val devices = if (state.selectedSeries.isNotEmpty()) VivoDeviceDatabase.devicesOf(state.selectedSeries) else emptyList()
                            AnimatedVisibility(
                                visible = devices.isNotEmpty(),
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                ModelDropdownCard(state, viewModel)
                            }
                        }
                    }
                }

                item { DeviceTypeCard(state, viewModel) }
                item { AndroidVersionCard(state, viewModel) }

                if (codename.isNotEmpty()) {
                    item { CodenameInfoCard(state) }
                }

                item { VersionInputCard(state, viewModel) }
                item { SnInputCard(state, viewModel) }
                item { PackageTypeCard(state, viewModel) }

                item { QueryButton(state, viewModel) }

                state.error?.let { err -> item { ErrorCard(err) } }
                state.result?.let { result -> item { ResultCard(result, state.changelogContent, state.softwareVersion) } }
                if (state.history.isNotEmpty()) {
                    item { HistoryCard(state.history, viewModel) }
                }
            }
        }

        if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun CryptoStatusCard(ready: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Text(
            text = if (ready) stringResource(R.string.crypto_ready) else stringResource(R.string.crypto_init),
            color = if (ready) Color(0xFF4CAF60) else Color(0xFFFF9800),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ManualModeCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                state = if (state.manualMode) ToggleableState.On else ToggleableState.Off,
                onClick = { viewModel.toggleManualMode() }
            )
            Text(
                text = stringResource(R.string.manual_input),
                modifier = Modifier.padding(start = 8.dp).clickable { viewModel.toggleManualMode() }
            )
        }
    }
}

@Composable
private fun ManualInputCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column {
            TextField(
                insideMargin = DpSize(16.dp, 24.dp),
                modifier = Modifier.fillMaxWidth(),
                value = state.manualCodename,
                onValueChange = { viewModel.updateManualCodename(it) },
                label = stringResource(R.string.label_codename),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            TextField(
                insideMargin = DpSize(16.dp, 24.dp),
                modifier = Modifier.fillMaxWidth(),
                value = state.manualModelSwVer,
                onValueChange = { viewModel.updateManualModelSwVer(it) },
                label = stringResource(R.string.label_model_sw_ver),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }
    }
}

@Composable
private fun SeriesDropdownCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    val seriesList = VivoDeviceDatabase.series
    val seriesIndex = seriesList.indexOf(state.selectedSeries).takeIf { it >= 0 } ?: 0
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        OverlayDropdownPreference(
            title = stringResource(R.string.label_series),
            items = seriesList,
            selectedIndex = seriesIndex,
            onSelectedIndexChange = { viewModel.selectSeries(seriesList[it]) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModelDropdownCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    val devices = VivoDeviceDatabase.devicesOf(state.selectedSeries)
    val deviceNames = devices.map { "${it.model} (${it.model_sw_ver})" }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        OverlayDropdownPreference(
            title = stringResource(R.string.label_model),
            items = deviceNames,
            selectedIndex = state.selectedModelIndex,
            onSelectedIndexChange = { viewModel.selectDevice(it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DeviceTypeCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    val phoneStr = stringResource(R.string.device_phone)
    val tabletStr = stringResource(R.string.device_tablet)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        OverlayDropdownPreference(
            title = stringResource(R.string.label_device_type),
            items = listOf(phoneStr, tabletStr),
            selectedIndex = if (state.deviceType == "phone") 0 else 1,
            onSelectedIndexChange = { viewModel.updateDeviceType(if (it == 0) "phone" else "tablet") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AndroidVersionCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    val androidVersions = listOf("13", "14", "15", "16")
    val androidIndex = androidVersions.indexOf(state.androidVersion.toString()).takeIf { it >= 0 } ?: 3
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        OverlayDropdownPreference(
            title = stringResource(R.string.label_android_version),
            items = androidVersions,
            selectedIndex = androidIndex,
            onSelectedIndexChange = { viewModel.updateAndroidVersion(androidVersions[it].toInt()) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CodenameInfoCard(state: VivoOtaUiState) {
    val codename = if (state.manualMode) state.manualCodename else state.selectedCodename
    val model = if (state.manualMode) state.manualModelName.ifEmpty { codename } else state.selectedModel
    val swVer = if (state.manualMode) state.manualModelSwVer else state.selectedModelSwVer
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.display_model, model))
            Text(stringResource(R.string.display_codename, codename), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            if (swVer.isNotEmpty()) {
                Text(stringResource(R.string.display_sw_ver, swVer), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    }
}

@Composable
private fun VersionInputCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column {
            TextField(
                insideMargin = DpSize(16.dp, 24.dp),
                modifier = Modifier.fillMaxWidth(),
                value = state.softwareVersion,
                onValueChange = { viewModel.updateSoftwareVersion(it) },
                label = stringResource(R.string.label_sw_version),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\u26A0", fontSize = 13.sp, color = Color(0xFFFF9800))
                Text(
                    text = stringResource(R.string.hint_sw_version),
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SnInputCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        TextField(
            insideMargin = DpSize(16.dp, 24.dp),
            modifier = Modifier.fillMaxWidth(),
            value = state.sn,
            onValueChange = { viewModel.updateSn(it) },
            label = stringResource(R.string.label_sn),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.query() })
        )
    }
}

@Composable
private fun PackageTypeCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    val fullPkg = stringResource(R.string.pkg_full)
    val incrementalPkg = stringResource(R.string.pkg_incremental)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        OverlayDropdownPreference(
            title = stringResource(R.string.label_package_type),
            items = listOf(fullPkg, incrementalPkg),
            selectedIndex = if (state.isFullPackage) 0 else 1,
            onSelectedIndexChange = { viewModel.togglePackageType() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QueryButton(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    val codename = if (state.manualMode) state.manualCodename else state.selectedCodename
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.query() },
            enabled = state.cryptoReady && !state.isLoading && codename.isNotEmpty() && state.softwareVersion.isNotEmpty(),
            colors = ButtonDefaults.buttonColorsPrimary()
        ) {
            if (state.isLoading) {
                Text(stringResource(R.string.querying))
            } else {
                Text(stringResource(R.string.btn_query))
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Text(
            text = stringResource(R.string.query_failed, error),
            color = Color(0xFFE53935),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ResultCard(result: VivoOtaResult, changelogContent: String?, currentVersion: String) {
    val context = LocalContext.current
    val copiedMsg = stringResource(R.string.copied)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.result_title), fontSize = 16.sp)
            if (currentVersion.isNotEmpty()) {
                Text(stringResource(R.string.current_version, currentVersion), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            if (result.updateVersion.isNotEmpty() && result.updateVersion != "(Not found)") {
                Text(stringResource(R.string.latest_version, result.updateVersion))
            }
            if (result.securityPatch.isNotEmpty() && result.securityPatch != "(Not found)") {
                Text(stringResource(R.string.security_patch, result.securityPatch), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            if (result.updateDate.isNotEmpty() && result.updateDate != "(Not found)") {
                Text(stringResource(R.string.update_date, result.updateDate), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            if (result.filename.isNotEmpty() && result.filename != "(Not found)") {
                Text(stringResource(R.string.filename, result.filename), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            if (result.fileSizeMb.isNotEmpty()) {
                Text(stringResource(R.string.size_mb, result.fileSizeMb), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            if (result.downloadUrl.isNotEmpty()) {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.downloadUrl)))
                    }) { Text(stringResource(R.string.btn_download)) }
                    Button(onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("url", result.downloadUrl))
                        Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                    }) { Text(stringResource(R.string.btn_copy_link)) }
                }
            }
            if (changelogContent != null) {
                HorizontalDivider()
                Text(stringResource(R.string.changelog_title), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (changelogContent == "loading") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.loading), modifier = Modifier.padding(start = 12.dp), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                } else {
                    Text(
                        text = changelogContent,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(history: List<QueryHistoryEntry>, viewModel: VivoOtaViewModel) {
    val context = LocalContext.current
    val copiedMsg = stringResource(R.string.copied)
    val copyLinkStr = stringResource(R.string.btn_copy_link)
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.history_title, history.size), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (expanded) " ▾" else " ▸",
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.btn_clear),
                    color = Color(0xFFE53935),
                    modifier = Modifier.clickable { viewModel.clearHistory() }
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider()
                    history.take(10).forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("${entry.model} · ${viewModel.formatTime(entry.timestamp)}")
                            Text(
                                stringResource(R.string.history_query, entry.swVersion, entry.resultVersion),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 13.sp
                            )
                            if (entry.fileSize.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.history_size, entry.fileSize),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    fontSize = 13.sp
                                )
                            }
                            if (entry.downloadUrl.isNotEmpty()) {
                                Text(
                                    text = copyLinkStr,
                                    color = MiuixTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("url", entry.downloadUrl))
                                        Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(stringResource(R.string.app_name), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("v1.2.0", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(stringResource(R.string.about_developer), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("mytiantian001", fontSize = 13.sp)
                Text(
                    text = "Coolapk @mytiantian",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/4430874")))
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(stringResource(R.string.about_original_author), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Coolapk @JerryTse", fontSize = 13.sp)
                Text(
                    text = "GitHub: JerryTse-OSS / VIVO-OTA-Tracker",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/JerryTse-OSS/VIVO-OTA-Tracker")))
                    }
                )
                Text(
                    text = "Coolapk: @JerryTse",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/2643293")))
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(stringResource(R.string.about_reference), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "YuKongA / Updater-KMP",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/YuKongA/Updater-KMP")))
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(stringResource(R.string.about_source), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "VIVO-OTA-Tracker-Android",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mytiantian001/VIVO-OTA-Tracker-Android")))
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "© 2026 mytiantian001",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Text(
                    stringResource(R.string.about_disclaimer),
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}
