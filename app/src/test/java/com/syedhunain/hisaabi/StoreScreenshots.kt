package com.syedhunain.hisaabi

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.syedhunain.hisaabi.data.Khata
import com.syedhunain.hisaabi.ui.Str
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Clean demo data for Google Play store screenshots (saved as build/screens/S_*.png). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class StoreScreenshots : ShotsBase() {

    private fun seedDemo() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        ui {
            vm.prefs.markWelcomeSeen()
            vm.prefs.updateBusiness("Raza General Store")
            vm.saveKhata(null, "Ahmed Raza", "0300 1234567", Khata.TYPE_CUSTOMER, "", "", 3000, true) { id ->
                vm.saveTxn(null, id, 6000, true, "Saman — 2 cartons", now - 5 * day)
                vm.saveTxn(null, id, 2500, false, "Cash wapsi", now - 2 * day)
                vm.saveTxn(null, id, 1500, true, "Mobile repair", now - 3 * 3_600_000L)
            }
            vm.saveKhata(null, "Bilal Traders", "0321 7654321", Khata.TYPE_SUPPLIER, "", "", 12300, false) { id ->
                vm.saveTxn(null, id, 5000, false, "Maal — chawal", now - 35 * day)
                vm.saveTxn(null, id, 8000, true, "Payment Easypaisa", now - 10 * day)
            }
            vm.saveKhata(null, "Sana Ali", "0333 1122334", Khata.TYPE_FRIEND, "", "", 4000, true) { id ->
                vm.saveTxn(null, id, 2000, false, "Wapas kiye", now - 65 * day)
            }
            vm.saveKhata(null, "Usman Khan", "", Khata.TYPE_CUSTOMER, "", "", 20000, true) { id ->
                vm.saveTxn(null, id, 7000, false, "Kist #1", now - 95 * day)
                vm.saveTxn(null, id, 4500, true, "Udhaar", now - 125 * day)
            }
            vm.saveKhata(null, "Hamza Motors", "0345 9988776", Khata.TYPE_SUPPLIER, "", "", 9500, false) { id ->
                vm.saveTxn(null, id, 3000, true, "Service", now - 150 * day)
            }
            vm.saveKhata(null, "Faisal Bhai", "", Khata.TYPE_PERSONAL, "", "", 0, true) { id ->
                vm.saveTxn(null, id, 1200, true, "Khana", now - day)
                vm.saveTxn(null, id, 1200, false, "Wapas", now - 12 * 3_600_000L)
            }
        }
        rule.waitUntil(15_000) { vm.khatas.value.size >= 6 && vm.txns.value.size >= 16 }
        waitForText("Ahmed Raza")
    }

    @Test
    fun storeShots() {
        val s = Str(true)
        shot("S_00_welcome")
        seedDemo()

        shot("S_01_home")
        click("Ahmed Raza")
        shot("S_02_khata")
        click(s.gave)
        rule.onAllNodes(hasSetTextAction())[0].performTextInput("3500")
        rule.onAllNodes(hasSetTextAction())[1].performTextInput("Doodh aur cheeni")
        shot("S_03_entry")
        back()
        back()
        click(s.tabReports)
        shot("S_04_reports")
        click(s.tabKhatay)
        click(s.newKhata)
        shot("S_05_new_khata")
        back()

        ui { vm.prefs.setGoogleAccount("aap@gmail.com", "Aap"); vm.prefs.markBackup(System.currentTimeMillis()) }
        click(s.tabSettings)
        shot("S_06_settings")
        click(s.tabKhatay)

        ui { vm.prefs.updateDark(true) }
        rule.waitForIdle()
        shot("S_07_home_dark")
        click("Ahmed Raza")
        shot("S_08_khata_dark")
        ui { vm.prefs.updateDark(false) }
    }
}
