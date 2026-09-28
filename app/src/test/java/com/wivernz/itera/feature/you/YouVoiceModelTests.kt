package com.wivernz.itera.feature.you

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.voice.OfflineModel
import com.wivernz.itera.core.voice.VoiceModelImport
import com.wivernz.itera.core.voice.VoiceModelInfo
import com.wivernz.itera.core.voice.VoiceModelSource
import com.wivernz.itera.core.voice.VoiceModelState
import com.wivernz.itera.core.voice.VoiceModelStore
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.subscribe
import com.wivernz.itera.feature.voice.OfflineModelSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Milestone 013: the model store as Settings sees it; tests set states and record actions. */
class FakeVoiceModelStore : VoiceModelStore {
    override val states =
        MutableStateFlow(
            VoiceLanguage.entries.associateWith<VoiceLanguage, VoiceModelState> {
                VoiceModelState.NotInstalled
            }
        )
    override val downloadAvailable = MutableStateFlow(false)
    val downloads = mutableListOf<VoiceLanguage>()
    val removals = mutableListOf<VoiceLanguage>()
    val imports = mutableListOf<String>()
    var importResult = VoiceModelImport.INSTALLED

    override fun info(language: VoiceLanguage) =
        VoiceModelInfo("vosk-model-small-${language.tag}.zip", 46_000_000)

    override fun download(language: VoiceLanguage) {
        downloads += language
    }

    override suspend fun import(uri: String): VoiceModelImport {
        imports += uri
        return importResult
    }

    override fun remove(language: VoiceLanguage) {
        removals += language
    }

    override fun installed(language: VoiceLanguage): OfflineModel? = null

    override fun reportLoadFailure(language: VoiceLanguage, error: Throwable) = Unit

    val rechecks = mutableListOf<VoiceLanguage>()

    override fun revalidate(language: VoiceLanguage) {
        rechecks += language
    }
}

@RunWith(RobolectricTestRunner::class)
class YouVoiceModelTest : YouTestBase() {
    @Test fun modelActionsReachTheStoreAndNothingDownloadsByItself() = runBlocking {
        val vm = you()
        val stop = vm.state.subscribe()
        try {
            val loaded = vm.state.await { !it.loading }
            assertTrue("never automatic", models.downloads.isEmpty())
            assertEquals(VoiceModelState.NotInstalled, loaded.voiceModels[VoiceLanguage.RU])
            assertEquals(
                "vosk-model-small-ru-RU.zip",
                loaded.voiceModelInfo[VoiceLanguage.RU]?.file
            )
            vm.onEvent(YouUiEvent.DownloadModel(VoiceLanguage.RU))
            vm.onEvent(YouUiEvent.ImportModel("content://picked"))
            vm.onEvent(YouUiEvent.RemoveModel(VoiceLanguage.DE))
            vm.onEvent(YouUiEvent.RecheckModel(VoiceLanguage.RU))
            // the events run on the view model's scope; wait for the last one to land
            repeat(100) {
                if (models.removals.isNotEmpty()) return@repeat
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                Thread.sleep(10)
            }
            assertEquals(listOf(VoiceLanguage.RU), models.downloads)
            assertEquals(listOf("content://picked"), models.imports)
            assertEquals(listOf(VoiceLanguage.DE), models.removals)
            repeat(100) {
                if (models.rechecks.isNotEmpty()) return@repeat
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                Thread.sleep(10)
            }
            assertEquals(listOf(VoiceLanguage.RU), models.rechecks)
            models.states.value =
                models.states.value + (VoiceLanguage.RU to VoiceModelState.Downloading(30))
            vm.state.await { it.voiceModels[VoiceLanguage.RU] == VoiceModelState.Downloading(30) }
            Unit
        } finally {
            stop()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class OfflineModelSheetTest {
    @get:Rule val compose = createComposeRule()

    private val info = VoiceModelInfo("vosk-model-small-ru-0.22.zip", 46_236_750)

    private fun show(state: VoiceModelState, downloadable: Boolean, actions: MutableList<String>) {
        compose.setContent {
            IteraTheme {
                OfflineModelSheet(
                    VoiceLanguage.RU,
                    state,
                    info,
                    downloadable,
                    onDownload = { actions += "download" },
                    onImport = { actions += "import" },
                    onRemove = { actions += "remove" },
                    onDismiss = { actions += "dismiss" },
                    onRecheck = { actions += "recheck" }
                )
            }
        }
    }

    @Test fun sideloadedBuildsOfferImportWithTheExactFile() {
        val actions = mutableListOf<String>()
        show(VoiceModelState.NotInstalled, downloadable = false, actions)
        compose.onNodeWithText("Download").assertDoesNotExist()
        compose.onNodeWithText("vosk-model-small-ru-0.22.zip", substring = true).assertExists()
        compose.onNodeWithText("Import file").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("import"), actions) }
    }

    @Test fun playBuildsOfferDownloadAndImport() {
        val actions = mutableListOf<String>()
        show(VoiceModelState.NotInstalled, downloadable = true, actions)
        compose.onNodeWithText("Not installed", substring = true).assertExists()
        compose.onNodeWithText("Download").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("download"), actions) }
    }

    @Test fun anInstalledModelCanBeRemoved() {
        val actions = mutableListOf<String>()
        show(
            VoiceModelState.Installed(
                "vosk-model-small-ru-0.22",
                91_289_240,
                VoiceModelSource.IMPORTED
            ),
            false,
            actions
        )
        compose.onNodeWithText("Installed", substring = true).assertExists()
        compose.onNodeWithText("Import file").assertDoesNotExist()
        compose.onNodeWithText("Remove").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("remove", "dismiss"), actions) }
    }

    @Test fun progressAndDamageAreShown() {
        show(VoiceModelState.Downloading(40), true, mutableListOf())
        compose.onNodeWithText("Downloading · 40%").assertExists()
        compose.onNodeWithText("Download").assertDoesNotExist()
        compose.onNodeWithTag("OfflineModelSheet").assertExists()
    }

    @Test fun anUnusableModelOffersCheckAgainAndKeepsRemove() {
        val actions = mutableListOf<String>()
        show(VoiceModelState.Unusable, downloadable = false, actions)
        compose.onNodeWithText("Couldn't be loaded · check again").assertExists()
        compose.onNodeWithText("Import file").assertDoesNotExist()
        compose.onNodeWithText("Check again").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("recheck"), actions) }
        compose.onNodeWithText("Remove").assertExists()
    }

    @Test fun aDamagedModelOffersCheckAgainAndReinstall() {
        val actions = mutableListOf<String>()
        show(VoiceModelState.Damaged, downloadable = true, actions)
        compose.onNodeWithText("Check again").assertExists()
        compose.onNodeWithText("Download").assertExists()
        compose.onNodeWithText("Import file").assertExists()
    }

    @Test fun validationIsNeverShownAsReady() {
        show(VoiceModelState.Validating, downloadable = true, mutableListOf())
        compose.onNodeWithText("Checking the model…").assertExists()
        compose.onNodeWithText("Installed", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Download").assertDoesNotExist()
        compose.onNodeWithText("Remove").assertDoesNotExist()
    }
}
