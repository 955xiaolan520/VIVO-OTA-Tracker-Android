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

    MiuixTheme(if (darkMode) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                SmallTopAppBar(
                    title = "VIVO OTA Tracker",
                    navigationIcon = {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher),
                            contentDescription = "About",
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { CryptoStatusCard(state.cryptoReady) }
                item { DeviceSelectCard(state, viewModel) }
                item { VersionInputCard(state, viewModel) }
                item { QueryButton(state, viewModel) }
                state.error?.let { err -> item { ErrorCard(err) } }
                state.result?.let { result -> item { ResultCard(result, state.changelogContent) } }
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
            text = if (ready) "● 加密引擎就绪" else "● 正在初始化加密引擎...",
            color = if (ready) Color(0xFF4CAF60) else Color(0xFFFF9800),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun DeviceSelectCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                state = if (state.manualMode) ToggleableState.On else ToggleableState.Off,
                onClick = { viewModel.toggleManualMode() }
            )
            Text(
                text = "手动输入设备信息",
                modifier = Modifier.padding(start = 8.dp).clickable { viewModel.toggleManualMode() }
            )
        }

        val seriesList = VivoDeviceDatabase.series
        val seriesIndex = seriesList.indexOf(state.selectedSeries).takeIf { it >= 0 } ?: 0
        val devices = if (state.selectedSeries.isNotEmpty()) VivoDeviceDatabase.devicesOf(state.selectedSeries) else emptyList()
        val deviceNames = devices.map { "${it.model} (${it.model_sw_ver})" }

        AnimatedVisibility(
            visible = state.manualMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider()
                TextField(
                    insideMargin = DpSize(16.dp, 20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    value = state.manualCodename,
                    onValueChange = { viewModel.updateManualCodename(it) },
                    label = "代号 (如: PD2408)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                HorizontalDivider()
                TextField(
                    insideMargin = DpSize(16.dp, 20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    value = state.manualModelSwVer,
                    onValueChange = { viewModel.updateManualModelSwVer(it) },
                    label = "公开型号 (如: V2408A)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }
        }

        AnimatedVisibility(
            visible = !state.manualMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider()
                OverlayDropdownPreference(
                    title = "系列",
                    items = seriesList,
                    selectedIndex = seriesIndex,
                    onSelectedIndexChange = { viewModel.selectSeries(seriesList[it]) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (deviceNames.isNotEmpty()) {
                    HorizontalDivider()
                    OverlayDropdownPreference(
                        title = "型号",
                        items = deviceNames,
                        selectedIndex = state.selectedModelIndex,
                        onSelectedIndexChange = { viewModel.selectDevice(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        HorizontalDivider()
        OverlayDropdownPreference(
            title = "设备类型",
            items = listOf("手机 (phone)", "平板 (tablet)"),
            selectedIndex = if (state.deviceType == "phone") 0 else 1,
            onSelectedIndexChange = { viewModel.updateDeviceType(if (it == 0) "phone" else "tablet") },
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider()
        val androidVersions = listOf("13", "14", "15", "16")
        val androidIndex = androidVersions.indexOf(state.androidVersion.toString()).takeIf { it >= 0 } ?: 3
        OverlayDropdownPreference(
            title = "Android 版本",
            items = androidVersions,
            selectedIndex = androidIndex,
            onSelectedIndexChange = { viewModel.updateAndroidVersion(androidVersions[it].toInt()) },
            modifier = Modifier.fillMaxWidth()
        )

        val codename = if (state.manualMode) state.manualCodename else state.selectedCodename
        if (codename.isNotEmpty()) {
            HorizontalDivider()
            Column(modifier = Modifier.padding(16.dp)) {
                val model = if (state.manualMode) state.manualModelName.ifEmpty { codename } else state.selectedModel
                val swVer = if (state.manualMode) state.manualModelSwVer else state.selectedModelSwVer
                Text("型号: $model")
                Text("代号: $codename", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                if (swVer.isNotEmpty()) {
                    Text("软件版本号: $swVer", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
    }
}

@Composable
private fun VersionInputCard(state: VivoOtaUiState, viewModel: VivoOtaViewModel) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        TextField(
            insideMargin = DpSize(16.dp, 20.dp),
            modifier = Modifier.fillMaxWidth(),
            value = state.softwareVersion,
            onValueChange = { viewModel.updateSoftwareVersion(it) },
            label = "系统版本号 (例如: 16.1.16.5.W10)",
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        HorizontalDivider()
        TextField(
            insideMargin = DpSize(16.dp, 20.dp),
            modifier = Modifier.fillMaxWidth(),
            value = state.sn,
            onValueChange = { viewModel.updateSn(it) },
            label = "SN 序列号 (可选)",
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.query() })
        )
        HorizontalDivider()
        OverlayDropdownPreference(
            title = "包类型",
            items = listOf("完整包", "增量包"),
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
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text("查询中...", modifier = Modifier.padding(start = 8.dp))
            } else {
                Text("查询 OTA 更新")
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Text(
            text = "查询失败: $error",
            color = Color(0xFFE53935),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ResultCard(result: VivoOtaResult, changelogContent: String?) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("查询结果", fontSize = 16.sp)
            if (result.updateVersion.isNotEmpty() && result.updateVersion != "(Not found)") {
                Text("最新版本: ${result.updateVersion}")
            }
            if (result.filename.isNotEmpty() && result.filename != "(Not found)") {
                Text("文件名: ${result.filename}")
            }
            if (result.fileSizeMb.isNotEmpty()) {
                Text("大小: ${result.fileSizeMb} MB")
            }
            if (result.downloadUrl.isNotEmpty()) {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.downloadUrl)))
                    }) { Text("下载") }
                    Button(onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("url", result.downloadUrl))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }) { Text("复制链接") }
                }
            }
            if (changelogContent != null) {
                HorizontalDivider()
                Text("更新日志", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (changelogContent == "loading") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("加载中...", modifier = Modifier.padding(start = 8.dp), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
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
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("查询历史 (${history.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "清空",
                color = Color(0xFFE53935),
                modifier = Modifier.clickable { viewModel.clearHistory() }
            )
        }
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
                    "查询: ${entry.swVersion} → ${entry.resultVersion}",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp
                )
                if (entry.fileSize.isNotEmpty()) {
                    Text(
                        "大小: ${entry.fileSize} MB",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 13.sp
                    )
                }
                if (entry.downloadUrl.isNotEmpty()) {
                    Text(
                        text = "复制链接",
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("url", entry.downloadUrl))
                            Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                        }
                    )
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
                Text("VIVO OTA Tracker", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("v1.1.0", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text("开发者", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("mytiantian001", fontSize = 13.sp)
                Text(
                    text = "酷安 @mytiantian_是天天吖",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/4430874")))
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text("原项目作者", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("酷安 @桜酱没有未来", fontSize = 13.sp)
                Text(
                    text = "GitHub: JerryTse-OSS / VIVO-OTA-Tracker",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/JerryTse-OSS/VIVO-OTA-Tracker")))
                    }
                )
                Text(
                    text = "酷安: @桜酱没有未来",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coolapk.com/u/2643293")))
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text("参考项目", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "YuKongA / Updater-KMP",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/YuKongA/Updater-KMP")))
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text("源码仓库", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    "本应用仅供学习研究使用",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("关闭")
                }
            }
        }
    }
}
