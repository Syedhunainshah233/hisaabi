package com.syedhunain.hisaabi.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.syedhunain.hisaabi.data.Khata

/** All app text in English and Roman Urdu. */
class Str(val romanUrdu: Boolean) {
    private fun t(en: String, ur: String) = if (romanUrdu) ur else en

    // Tabs
    val tabKhatay get() = t("Ledgers", "Khatay")
    val tabReports get() = t("Reports", "Hisaab")
    val tabSettings get() = t("Settings", "Settings")

    // Home
    val youWillGet get() = t("You will get", "Aap ne lene hain")
    val youWillGive get() = t("You will give", "Aap ne dene hain")
    val netBalance get() = t("Net balance", "Net baqi")
    val filterAll get() = t("All", "Sab")
    val filterGet get() = t("To get", "Lene hain")
    val filterGive get() = t("To give", "Dene hain")
    val settled get() = t("Settled", "Barabar")
    val newKhata get() = t("New Ledger", "Naya Khata")
    val searchHint get() = t("Search name or number", "Naam ya number talash karein")
    val searchOpen get() = t("Search", "Talash karein")
    val searchClose get() = t("Close search", "Talash band karein")
    val emptyHome get() = t(
        "No ledgers yet.\nTap \"New Ledger\" to add your first one.",
        "Abhi koi khata nahi.\n\"Naya Khata\" daba kar pehla khata banayein."
    )
    val noResults get() = t("Nothing found", "Kuch nahi mila")
    val today get() = t("Today", "Aaj")
    val yesterday get() = t("Yesterday", "Kal")
    fun entriesCount(n: Int) = t("$n entries", "$n entries")

    // Khata detail
    val gave get() = t("Gave", "Diya")
    val got get() = t("Got", "Liya")
    val colEntries get() = t("ENTRIES", "ENTRIES")
    val colGave get() = t("GAVE", "DIYA")
    val colGot get() = t("GOT", "LIYA")
    val noEntries get() = t(
        "No entries yet.\nUse Gave / Got below.",
        "Abhi koi entry nahi.\nNeeche Diya / Liya dabayein."
    )
    val reminder get() = t("Reminder", "Reminder")
    val pdfStatement get() = t("PDF Statement", "PDF Statement")
    val call get() = t("Call", "Call karein")
    val edit get() = t("Edit", "Badlein")
    val more get() = t("More options", "Mazeed options")
    val back get() = t("Back", "Wapas")
    val openingBalance get() = t("Opening balance", "Opening balance")
    fun noEntry(gave: Boolean) = if (gave) t("Gave", "Diya") else t("Got", "Liya")
    val deleteKhataTitle get() = t("Delete this ledger?", "Yeh khata delete karein?")
    val deleteKhataBody get() = t(
        "All entries of this ledger will be deleted. This cannot be undone.",
        "Is khate ki saari entries bhi mit jayengi. Yeh wapas nahi hoga."
    )
    val noPhone get() = t("No phone number saved", "Koi number save nahi")

    fun reminderMessage(name: String, amount: String, business: String): String =
        if (romanUrdu) {
            val who = if (business.isBlank()) "" else " $business ke"
            "Assalam o Alaikum $name,\nAap ki taraf$who $amount baqi hain. Meherbani kar ke jald ada kar dein.\nShukriya!"
        } else {
            val who = if (business.isBlank()) "" else " to $business"
            "Hello $name,\nA balance of $amount is pending$who. Kindly clear it at your earliest.\nThank you!"
        }

    // Entry form
    fun gaveTo(name: String) = t("Gave to $name", "$name ko diya")
    fun gotFrom(name: String) = t("Got from $name", "$name se liya")
    val amount get() = t("Amount", "Raqam")
    val date get() = t("Date", "Tareekh")
    val noteOptional get() = t("Note (optional)", "Note (optional)")
    val noteHint get() = t("e.g. mobile repair", "Jaise: mobile repair ka udhaar")
    val balanceAfter get() = t("Balance after this", "Is ke baad baqi")
    val save get() = t("Save", "Mehfooz Karein")
    val saved get() = t("Saved!", "Mehfooz ho gaya!")
    val delete get() = t("Delete", "Delete")
    val cancel get() = t("Cancel", "Cancel")
    val ok get() = t("OK", "Theek hai")
    val close get() = t("Close", "Band karein")
    val amountError get() = t("Enter an amount", "Raqam likhein")
    val deleteEntryTitle get() = t("Delete this entry?", "Yeh entry delete karein?")
    fun willGet(amount: String) = t("$amount to get", "$amount lene hain")
    fun willGive(amount: String) = t("$amount to give", "$amount dene hain")

    // Khata form
    val createKhata get() = t("Create Ledger", "Naya Khata Banayein")
    val editKhata get() = t("Edit Ledger", "Khata Badlein")
    val name get() = t("Name *", "Naam *")
    val nameError get() = t("Name is required", "Naam zaroori hai")
    val mobile get() = t("Mobile number", "Mobile number")
    val contacts get() = t("Contacts", "Contacts")
    val khataType get() = t("Ledger type", "Khate ki qism")
    val openingOptional get() = t("Opening balance (optional)", "Opening balance (optional)")
    val iWillGet get() = t("I will get", "Maine lene hain")
    val iWillGive get() = t("I will give", "Maine dene hain")
    val address get() = t("Address (optional)", "Address (optional)")
    val createBtn get() = t("Create Ledger", "Khata Banayein")

    fun typeLabel(type: String) = when (type) {
        Khata.TYPE_SUPPLIER -> t("Supplier", "Supplier")
        Khata.TYPE_FRIEND -> t("Friend / Family", "Dost / Rishtedar")
        Khata.TYPE_PERSONAL -> t("Personal", "Personal")
        else -> t("Customer", "Customer")
    }

    // Reports
    val periodDay get() = t("Day", "Roz")
    val periodWeek get() = t("Week", "Hafta")
    val periodMonth get() = t("Month", "Mahina")
    val cameIn get() = t("Received (Got)", "Aaya (Liya)")
    val wentOut get() = t("Paid (Gave)", "Gaya (Diya)")
    val periodNet get() = t("Net for this period", "Is dauraan ka farq")
    val lastSixMonths get() = t("Last 6 months", "Pichle 6 mahine")
    val legendIn get() = t("In", "Aaya")
    val legendOut get() = t("Out", "Gaya")
    val periodEntries get() = t("Entries in this period", "Is dauraan ki entries")
    val noPeriodEntries get() = t("No entries in this period", "Is dauraan koi entry nahi")
    val previous get() = t("Previous", "Pichla")
    val next get() = t("Next", "Agla")
    val exportExcel get() = t("Export (Excel/CSV)", "Excel / CSV Export")

    // Settings
    val backupTitle get() = t("Google Drive Backup", "Google Drive Backup")
    val backupBody get() = t(
        "Sign in with Google to back up to your own Drive. Coming in the next version.",
        "Google se login kar ke apni Drive mein backup. Agle version mein aa raha hai."
    )
    val comingSoon get() = t("Coming soon", "Jald aa raha hai")
    val sectionData get() = t("DATA", "DATA")
    val sectionApp get() = t("APP", "APP")
    val sectionAbout get() = t("ABOUT", "HAMARE BAARE MEIN")
    val language get() = t("Language", "Zubaan")
    val darkMode get() = t("Dark mode", "Dark mode")
    val pinLock get() = t("PIN lock", "PIN lock")
    val currency get() = t("Currency", "Currency")
    val businessName get() = t("Business name", "Business ka naam")
    val businessHint get() = t("Shown on PDF statements", "PDF statement pe upar aayega")
    val notSet get() = t("Not set", "Nahi rakha")
    val version get() = t("Version", "Version")
    val developer get() = t("Developer", "Developer")
    val supportTitle get() = t("Support the developer", "Developer ko support karein")
    val supportBody get() = t(
        "Like Hisaabi? You can support its development by sending any amount to:",
        "Hisaabi pasand aayi? Koi bhi raqam bhej kar developer ki madad karein:"
    )
    val copy get() = t("Copy", "Copy")
    val copied get() = t("Account number copied", "Account number copy ho gaya")
    val setPin get() = t("Set a 4-digit PIN", "4 hindson ka PIN rakhein")
    val newPin get() = t("New PIN", "Naya PIN")
    val confirmPin get() = t("Confirm PIN", "PIN dobara likhein")
    val pinMismatch get() = t("PINs must be 4 digits and match", "PIN 4 hindson ka aur dono ek jaise hon")
    val nothingToExport get() = t("Nothing to export yet", "Abhi export karne ko kuch nahi")

    // Google backup
    val signInGoogle get() = t("Sign in with Google", "Google se Login karein")
    val startWithoutLogin get() = t("Start without signing in", "Bina login ke shuru karein")
    val welcomePrivacy get() = t(
        "Signing in backs up your records to your own Google Drive. We have no server — your data is only yours.",
        "Login karne se aap ka hisaab aap ki apni Google Drive mein backup hota hai. Hamara koi server nahi — data sirf aap ka."
    )
    val backupSignedOutBody get() = t(
        "Sign in with Google to back up to your own Drive. On a new phone, sign in and your records come back.",
        "Google se login kar ke apni Drive mein backup rakhein. Naye phone pe login karein aur sara hisaab wapas."
    )
    val connected get() = t("Connected to Google Drive", "Google Drive se juda hua")
    val lastBackup get() = t("Last backup", "Aakhri backup")
    val never get() = t("Not yet", "Abhi tak nahi")
    val backupNow get() = t("Back up now", "Abhi Backup")
    val restoreRow get() = t("Restore from backup", "Data wapas laayein (Restore)")
    val signOut get() = t("Sign out", "Logout")
    val autoBackupNote get() = t("Backs up automatically once a day when you open the app.", "Din mein ek dafa app kholne par khud backup ho jata hai.")
    val backupDone get() = t("Backup saved to Google Drive", "Google Drive mein backup ho gaya")
    val restoreDone get() = t("Your records are back", "Aap ka hisaab wapas aa gaya")
    val noBackup get() = t("No backup found in this Google account", "Is Google account mein koi backup nahi mila")
    val cloudError get() = t("Could not reach Google. Check internet and try again.", "Google se rabta nahi hua. Internet check kar ke dobara koshish karein.")
    val signInFailed get() = t("Google sign-in was cancelled", "Google login nahi hua")
    val backupFound get() = t("Backup found!", "Backup mil gaya!")
    fun backupFoundBody(date: String) = t("A Hisaabi backup from $date is in your Google Drive.", "Aap ki Google Drive mein $date ka Hisaabi backup mojood hai.")
    val restoreWarning get() = t(
        "Restoring replaces the records on this phone with the backup.",
        "Restore karne se is phone ka maujooda hisaab backup se badal jayega."
    )
    val restoreNow get() = t("Restore", "Wapas laayein")
    val notNow get() = t("Not now", "Abhi nahi")

    // Ads
    val adsOffTitle get() = t("Hide ads for 12 hours", "12 ghante ads band karein")
    val adsOffBody get() = t("Watch one full-screen video ad", "Ek poori video ad dekhein")
    fun adsOffUntil(time: String) = t("Ads off until $time", "Ads band hain: $time tak")
    val adsOffDone get() = t("Thanks! No ads for 12 hours", "Shukriya! 12 ghante koi ad nahi")
    val adNotReady get() = t("Video not available right now, try later", "Abhi video nahi mili, thori der baad koshish karein")

    // Lock
    val enterPin get() = t("Enter your PIN", "Apna PIN darj karein")
    val wrongPin get() = t("Wrong PIN, try again", "Ghalat PIN, dobara koshish karein")
    val deleteDigit get() = t("Delete digit", "Aakhri hindsa mitayein")

    // Misc
    val noApp get() = t("No app found for this", "Is kaam ke liye koi app nahi mili")
    val tagline get() = t("Your money, clearly counted.", "Aap ka lena dena, saaf aur mehfooz.")
}

val LocalStr = staticCompositionLocalOf { Str(true) }
