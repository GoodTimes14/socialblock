package it.socialblock.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import it.socialblock.PermissionActions
import it.socialblock.domain.model.AppSettings
import it.socialblock.domain.model.OverlayPosition
import it.socialblock.ui.screens.SettingsScreen
import it.socialblock.ui.theme.SocialBlockTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun selectsOverlayPosition() {
        var selectedPosition: OverlayPosition? = null
        composeRule.setContent {
            SocialBlockTheme {
                SettingsScreen(
                    state =
                        MainUiState(
                            isLoading = false,
                            settings =
                                AppSettings(
                                    overlayPosition = OverlayPosition.BOTTOM,
                                ),
                        ),
                    onBack = {},
                    onUpdateSummaryTime = { _, _ -> },
                    onUpdateOverlayPosition = { selectedPosition = it },
                    onUpdateOverlayOpacity = {},
                    onAddOverlayMessage = {},
                    onUpdateOverlayMessage = { _, _ -> },
                    onRemoveOverlayMessage = {},
                    onUpdateOverlayMessageChangeDelay = {},
                    permissionActions = PermissionActions({}, {}, {}),
                )
            }
        }

        composeRule.onNodeWithTag("overlay_position_bottom").assertIsSelected()
        composeRule.onNodeWithTag("overlay_position_top").performClick()
        assertEquals(OverlayPosition.TOP, selectedPosition)
    }

    @Test
    fun addsEditsAndRemovesOverlayMessages() {
        var addedMessage: String? = null
        var updatedMessage: Pair<Int, String>? = null
        var removedIndex: Int? = null
        composeRule.setContent {
            SocialBlockTheme {
                SettingsScreen(
                    state = settingsState(messages = listOf("Messaggio originale")),
                    onBack = {},
                    onUpdateSummaryTime = { _, _ -> },
                    onUpdateOverlayPosition = {},
                    onUpdateOverlayOpacity = {},
                    onAddOverlayMessage = { addedMessage = it },
                    onUpdateOverlayMessage = { index, message -> updatedMessage = index to message },
                    onRemoveOverlayMessage = { removedIndex = it },
                    onUpdateOverlayMessageChangeDelay = {},
                    permissionActions = PermissionActions({}, {}, {}),
                )
            }
        }

        composeRule.onNodeWithTag("add_overlay_message").performClick()
        composeRule.onNodeWithTag("overlay_message_text").performTextInput("Nuovo messaggio")
        composeRule.onNodeWithText("Salva").performClick()
        assertEquals("Nuovo messaggio", addedMessage)

        composeRule.onNodeWithContentDescription("Modifica messaggio 1").performClick()
        composeRule.onNodeWithTag("overlay_message_text").performTextReplacement("Messaggio aggiornato")
        composeRule.onNodeWithText("Salva").performClick()
        assertEquals(0 to "Messaggio aggiornato", updatedMessage)

        composeRule.onNodeWithContentDescription("Rimuovi messaggio 1").performClick()
        assertEquals(0, removedIndex)
    }

    @Test
    fun updatesOverlayMessageChangeDelay() {
        var delaySeconds: Int? = null
        composeRule.setContent {
            SocialBlockTheme {
                SettingsScreen(
                    state = settingsState(messages = emptyList()),
                    onBack = {},
                    onUpdateSummaryTime = { _, _ -> },
                    onUpdateOverlayPosition = {},
                    onUpdateOverlayOpacity = {},
                    onAddOverlayMessage = {},
                    onUpdateOverlayMessage = { _, _ -> },
                    onRemoveOverlayMessage = {},
                    onUpdateOverlayMessageChangeDelay = { delaySeconds = it },
                    permissionActions = PermissionActions({}, {}, {}),
                )
            }
        }

        composeRule.onNodeWithTag("overlay_message_delay_row").performClick()
        composeRule.onNodeWithTag("overlay_message_delay").performTextClearance()
        composeRule.onNodeWithTag("overlay_message_delay").performTextInput("45")
        composeRule.onNodeWithText("Salva").performClick()

        assertEquals(45, delaySeconds)
    }

    @Test
    fun updatesOverlayOpacity() {
        var opacityPercent: Int? = null
        composeRule.setContent {
            SocialBlockTheme {
                SettingsScreen(
                    state =
                        MainUiState(
                            isLoading = false,
                            settings = AppSettings(overlayOpacityPercent = 78),
                        ),
                    onBack = {},
                    onUpdateSummaryTime = { _, _ -> },
                    onUpdateOverlayPosition = {},
                    onUpdateOverlayOpacity = { opacityPercent = it },
                    onAddOverlayMessage = {},
                    onUpdateOverlayMessage = { _, _ -> },
                    onRemoveOverlayMessage = {},
                    onUpdateOverlayMessageChangeDelay = {},
                    permissionActions = PermissionActions({}, {}, {}),
                )
            }
        }

        composeRule.onNodeWithTag("overlay_opacity_row").performClick()
        composeRule.onNodeWithTag("overlay_opacity_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(55f) }
        composeRule.onNodeWithText("Salva").performClick()

        assertEquals(55, opacityPercent)
    }

    private fun settingsState(messages: List<String>) = MainUiState(
        isLoading = false,
        settings = AppSettings(overlayMessages = messages),
    )
}
