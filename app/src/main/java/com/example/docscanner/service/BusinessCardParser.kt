package com.example.docscanner.service

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import java.io.File
import java.io.FileOutputStream

fun BusinessCardParser.createAddContactIntent(contact: ExtractedContactData): Intent {
    return Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        contact.fullName?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
        contact.phone?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
        contact.alternatePhone?.let { putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, it) }
        contact.email?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
        contact.company?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
        contact.jobTitle?.let { putExtra(ContactsContract.Intents.Insert.JOB_TITLE, it) }
        contact.address?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
        contact.website?.let { putExtra(ContactsContract.Intents.Insert.NOTES, "Website: $it") }
    }
}

fun BusinessCardParser.writeVCardToFile(context: Context, contact: ExtractedContactData): File {
    val safeName = contact.fullName?.replace(Regex("""[^a-zA-Z0-9]"""), "_") ?: "contact"
    val file = File(context.cacheDir, "$safeName.vcf")
    FileOutputStream(file).use { out ->
        out.write(contact.toVCardString().toByteArray(Charsets.UTF_8))
    }
    return file
}
