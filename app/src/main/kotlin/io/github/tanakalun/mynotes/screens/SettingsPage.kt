package io.github.tanakalun.mynotes.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tanakalun.mynotes.R
import io.github.tanakalun.mynotes.SettingsStore
import io.github.tanakalun.mynotes.utils.BlurredBar
import io.github.tanakalun.mynotes.utils.PowerSaveModeTracker
import io.github.tanakalun.mynotes.utils.rememberBlurBackdrop
import androidx.compose.runtime.collectAsState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
private fun colorModeOptions(): List<String> = listOf(
    stringResource(R.string.color_mode_system),
    stringResource(R.string.color_mode_light),
    stringResource(R.string.color_mode_dark),
    stringResource(R.string.color_mode_monet_system),
    stringResource(R.string.color_mode_monet_light),
    stringResource(R.string.color_mode_monet_dark),
)

@Composable
private fun keyColorOptions(): List<String> = listOf(
    stringResource(R.string.key_color_default),
    stringResource(R.string.key_color_blue),
    stringResource(R.string.key_color_green),
    stringResource(R.string.key_color_purple),
    stringResource(R.string.key_color_yellow),
    stringResource(R.string.key_color_orange),
    stringResource(R.string.key_color_pink),
    stringResource(R.string.key_color_teal),
)

@Composable
private fun languageOptions(): List<String> = listOf(
    stringResource(R.string.lang_system),
    stringResource(R.string.lang_en),
    stringResource(R.string.lang_zh),
    stringResource(R.string.lang_zh_rTW),
    stringResource(R.string.lang_ja),
)

@Composable
private fun blurStyleOptions(): List<String> = listOf(
    stringResource(R.string.blur_style_gaussian),
    stringResource(R.string.blur_style_progressive),
)

@Composable
private fun navbarStyleOptions(): List<String> = listOf(
    stringResource(R.string.floating_navbar_style_default),
    stringResource(R.string.floating_navbar_style_ios),
)

@Composable
private fun navbarPositionOptions(): List<String> = listOf(
    stringResource(R.string.navbar_position_center),
    stringResource(R.string.navbar_position_start),
    stringResource(R.string.navbar_position_end),
)

private val PaletteStyleOptions = ThemePaletteStyle.entries.map { it.name }
private val ColorSpecOptions = ThemeColorSpec.entries.map { it.name }

@Composable
fun SettingsPage(
    innerPadding: PaddingValues,
    onAboutClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    importNoteCount: Int?,
    onImportConfirm: (Boolean) -> Unit,
    onImportDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val backdrop = rememberBlurBackdrop()
    val powerSave by PowerSaveModeTracker.isPowerSaveMode.collectAsState()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val ColorModeOptions = colorModeOptions()
    val KeyColorOptions = keyColorOptions()
    val LanguageOptions = languageOptions()
    val BlurStyleOptions = blurStyleOptions()
    val NavbarStyleOptions = navbarStyleOptions()
    val NavbarPositionOptions = navbarPositionOptions()

    Scaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                TopAppBar(
                    title = stringResource(R.string.settings),
                    color = barColor,
                    scrollBehavior = topAppBarScrollBehavior,
                )
            }
        },
    ) { scaffoldPadding ->
        val topPadding = scaffoldPadding.calculateTopPadding()
        val bottomPadding = innerPadding.calculateBottomPadding()

        Box(
            modifier = Modifier.fillMaxSize().then(
                if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier
            ),
        ) {
            LazyColumn(
                modifier = Modifier
                    .scrollEndHaptic()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = topPadding,
                    bottom = bottomPadding + 80.dp,
                ),
            ) {
                item(key = "appearance") {
                    SmallTitle(text = stringResource(R.string.section_appearance))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        OverlayDropdownPreference(
                            items = ColorModeOptions,
                            selectedIndex = SettingsStore.colorMode,
                            title = stringResource(R.string.color_mode),
                            summary = stringResource(R.string.color_mode_summary),
                            onSelectedIndexChange = { SettingsStore.updateColorMode(it) },
                        )
                        AnimatedVisibility(visible = SettingsStore.colorMode in 3..5) {
                            OverlayDropdownPreference(
                                items = KeyColorOptions,
                                selectedIndex = SettingsStore.keyColorIndex,
                                title = stringResource(R.string.key_color),
                                summary = stringResource(R.string.key_color_summary),
                                onSelectedIndexChange = { SettingsStore.updateKeyColorIndex(it) },
                            )
                        }
                        AnimatedVisibility(visible = SettingsStore.colorMode in 3..5 && SettingsStore.keyColorIndex > 0) {
                            OverlayDropdownPreference(
                                items = PaletteStyleOptions,
                                selectedIndex = SettingsStore.paletteStyle,
                                title = stringResource(R.string.palette_style),
                                summary = stringResource(R.string.palette_style_summary),
                                onSelectedIndexChange = { SettingsStore.updatePaletteStyle(it) },
                            )
                        }
                        AnimatedVisibility(visible = SettingsStore.colorMode in 3..5 && SettingsStore.keyColorIndex > 0) {
                            OverlayDropdownPreference(
                                items = ColorSpecOptions,
                                selectedIndex = SettingsStore.colorSpec,
                                title = stringResource(R.string.color_spec),
                                summary = stringResource(R.string.color_spec_summary),
                                onSelectedIndexChange = { SettingsStore.updateColorSpec(it) },
                            )
                        }
                        SwitchPreference(
                            title = stringResource(R.string.blur_effect),
                            summary = if (powerSave) {
                                stringResource(R.string.blur_effect_power_save)
                            } else {
                                stringResource(R.string.blur_effect_summary)
                            },
                            checked = SettingsStore.enableBlur && !powerSave,
                            enabled = !powerSave,
                            onCheckedChange = { SettingsStore.updateEnableBlur(it) },
                        )
                        OverlayDropdownPreference(
                            items = BlurStyleOptions,
                            selectedIndex = SettingsStore.blurStyle,
                            title = stringResource(R.string.blur_style),
                            summary = stringResource(R.string.blur_style_summary),
                            onSelectedIndexChange = { SettingsStore.updateBlurStyle(it) },
                            enabled = SettingsStore.enableBlur && !powerSave,
                        )
                        OverlayDropdownPreference(
                            items = LanguageOptions,
                            selectedIndex = SettingsStore.language,
                            title = stringResource(R.string.language),
                            summary = stringResource(R.string.language_summary),
                            onSelectedIndexChange = { SettingsStore.updateLanguage(context, it) },
                        )
                    }
                }

                item(key = "navigation") {
                    SmallTitle(text = stringResource(R.string.section_navigation))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.floating_navbar),
                            summary = stringResource(R.string.floating_navbar_summary),
                            checked = SettingsStore.useFloatingNavbar,
                            onCheckedChange = { SettingsStore.updateUseFloatingNavbar(it) },
                        )
                        AnimatedVisibility(visible = SettingsStore.useFloatingNavbar) {
                            Column {
                                OverlayDropdownPreference(
                                    items = NavbarStyleOptions,
                                    selectedIndex = SettingsStore.floatingNavbarStyle,
                                    title = stringResource(R.string.floating_navbar_style),
                                    summary = stringResource(R.string.floating_navbar_style_summary),
                                    onSelectedIndexChange = { SettingsStore.updateFloatingNavbarStyle(it) },
                                )
                                AnimatedVisibility(visible = SettingsStore.floatingNavbarStyle == 0) {
                                    OverlayDropdownPreference(
                                        items = NavbarPositionOptions,
                                        selectedIndex = SettingsStore.floatingNavbarPosition,
                                        title = stringResource(R.string.navbar_position),
                                        summary = stringResource(R.string.navbar_position_summary),
                                        onSelectedIndexChange = { SettingsStore.updateFloatingNavbarPosition(it) },
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "notes") {
                    SmallTitle(text = stringResource(R.string.section_notes))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.search_bar),
                            summary = stringResource(R.string.search_bar_summary),
                            checked = SettingsStore.showSearchBar,
                            onCheckedChange = { SettingsStore.updateShowSearchBar(it) },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.enter_creates_item),
                            summary = stringResource(R.string.enter_creates_item_summary),
                            checked = SettingsStore.enterCreatesItem,
                            onCheckedChange = { SettingsStore.updateEnterCreatesItem(it) },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.code_block_wrap),
                            summary = stringResource(R.string.code_block_wrap_summary),
                            checked = SettingsStore.codeBlockWrap,
                            onCheckedChange = { SettingsStore.updateCodeBlockWrap(it) },
                        )
                    }
                }

                item(key = "data") {
                    SmallTitle(text = stringResource(R.string.section_data))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.export_data),
                            summary = stringResource(R.string.export_data_summary),
                            onClick = onExportClick,
                        )
                        ArrowPreference(
                            title = stringResource(R.string.import_data),
                            summary = stringResource(R.string.import_data_summary),
                            onClick = onImportClick,
                        )
                    }
                }

                item(key = "about") {
                    SmallTitle(text = stringResource(R.string.section_other))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.about),
                            summary = stringResource(R.string.about_summary),
                            onClick = onAboutClick,
                        )
                    }
                }
            }
        }
    }

    if (importNoteCount != null) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.import_dialog_title),
            summary = stringResource(R.string.import_dialog_found, importNoteCount),
            onDismissRequest = onImportDismiss,
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.import_notes_settings),
                        onClick = { onImportConfirm(true) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        text = stringResource(R.string.import_notes_only),
                        onClick = { onImportConfirm(false) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = onImportDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }
}
