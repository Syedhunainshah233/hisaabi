package com.syedhunain.hisaabi

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.syedhunain.hisaabi.data.AppDatabase
import com.syedhunain.hisaabi.data.Khata
import com.syedhunain.hisaabi.ui.Str
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Walks through every screen and saves screenshots to app/build/screens.
 * Not pass/fail visual checks — they exist so a person can review layout,
 * colours (light + dark), popups and text overflow.
 */
@OptIn(ExperimentalRoborazziApi::class)
abstract class ShotsBase {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun closeDb() = AppDatabase.resetForTests()

    protected val vm get() = rule.activity.vm

    protected fun ui(block: () -> Unit) = rule.runOnUiThread(block)

    protected fun shot(name: String, idle: Boolean = true) {
        if (idle) rule.waitForIdle()
        captureScreenRoboImage("build/screens/$name.png")
    }

    protected fun click(text: String) {
        rule.onAllNodesWithText(text)[0].performClick()
        rule.waitForIdle()
    }

    protected fun back() {
        ui { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    protected fun waitForText(text: String) {
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun seed() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        ui {
            vm.saveKhata(null, "Ahmed Raza", "0300 1234567", Khata.TYPE_CUSTOMER, "", "", 3000, true) { id ->
                vm.saveTxn(null, id, 6000, true, "Saman — 2 cartons", now - 5 * day)
                vm.saveTxn(null, id, 2500, false, "Wapsi (cash)", now - 2 * day)
                vm.saveTxn(null, id, 1000, false, "Easypaisa se aaya", now - 40 * day)
            }
            vm.saveKhata(null, "Bilal Store", "03211234567", Khata.TYPE_SUPPLIER, "", "", 12300, false) { }
            vm.saveKhata(
                null, "Muhammad Usman Khan Traders & Sons Wholesale", "", Khata.TYPE_CUSTOMER, "", "", 1250000, true
            ) { id ->
                vm.saveTxn(
                    null, id, 98765, false,
                    "A very long note to check that long text never overflows the row layout",
                    now - 70 * day,
                )
            }
            vm.saveKhata(null, "Sana Ali", "", Khata.TYPE_FRIEND, "", "", 4000, true) { }
            vm.saveKhata(null, "Faisal", "", Khata.TYPE_PERSONAL, "", "", 0, true) { }
        }
        rule.waitUntil(15_000) { vm.txns.value.size >= 8 && vm.khatas.value.size >= 5 }
        waitForText("Ahmed Raza")
    }

    /** Visits the main screens in the current theme/language. */
    protected fun walk(tag: String, s: Str, full: Boolean) {
        shot("${tag}_01_home")

        click("Ahmed Raza")
        shot("${tag}_02_khata")

        if (full) {
            rule.onNodeWithContentDescription(s.delete).performClick()
            shot("${tag}_03_khata_delete_dialog")
            click(s.cancel)
        }

        click(s.gave)
        shot("${tag}_04_entry_empty")

        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("2000")
        fields[1].performTextInput("Mobile repair")
        shot("${tag}_05_entry_filled")

        if (full) {
            click(s.date)
            shot("${tag}_06_date_picker")
            click(s.cancel)
        }

        // Saved popup, caught mid-way before it closes.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(s.save).performClick()
        rule.mainClock.advanceTimeBy(500)
        shot("${tag}_07_saved_popup", idle = false)
        rule.mainClock.advanceTimeBy(1500)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        shot("${tag}_08_khata_after_save")

        back()
        click(s.newKhata)
        shot("${tag}_09_new_khata_form")
        back()

        click(s.tabReports)
        shot("${tag}_10_reports")

        click(s.tabSettings)
        shot("${tag}_11_settings")
        rule.onNodeWithText(s.developer).performScrollTo()
        shot("${tag}_12_settings_bottom")

        click(s.tabKhatay)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xhdpi")
class PhoneScreens : ShotsBase() {
    @Test
    fun romanUrdu_light_and_dark() {
        val s = Str(true)
        shot("A_00_home_empty_light")
        seed()

        walk("A_light", s, full = true)

        ui { vm.prefs.updateDark(true) }
        rule.waitForIdle()
        walk("B_dark", s, full = true)

        ui { vm.prefs.setPin("1234"); vm.unlocked = false }
        shot("B_dark_13_pin_lock")
        ui { vm.unlocked = true; vm.prefs.setPin(null); vm.prefs.updateDark(false) }
        rule.waitForIdle()
        shot("A_light_13_back_home")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h640dp-xhdpi")
class SmallScreenEnglish : ShotsBase() {
    @Test
    fun english_small_light_and_dark() {
        val s = Str(false)
        ui { vm.prefs.updateRomanUrdu(false) }
        seed()

        walk("C_small_en_light", s, full = false)

        ui { vm.prefs.updateDark(true) }
        rule.waitForIdle()
        walk("D_small_en_dark", s, full = false)

        ui { vm.prefs.updateRomanUrdu(true); vm.prefs.updateDark(false) }
    }
}
