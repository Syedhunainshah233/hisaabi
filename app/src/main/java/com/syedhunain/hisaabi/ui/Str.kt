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

    // Lock
    val enterPin get() = t("Enter your PIN", "Apna PIN darj karein")
    val wrongPin get() = t("Wrong PIN, try again", "Ghalat PIN, dobara koshish karein")
    val deleteDigit get() = t("Delete digit", "Aakhri hindsa mitayein")

    // Misc
    val noApp get() = t("No app found for this", "Is kaam ke liye koi app nahi mili")
    val tagline get() = t("Your money, clearly counted.", "Aap ka lena dena, saaf aur mehfooz.")
}

val LocalStr = staticCompositionLocalOf { Str(true) }
