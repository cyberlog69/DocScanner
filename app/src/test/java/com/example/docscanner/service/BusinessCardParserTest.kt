package com.example.docscanner.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessCardParserTest {

    @Test
    fun parse_standardBusinessCard_extractsAllFields() {
        val ocrText = """
            Alexander Hamilton
            Senior Software Architect
            Apex Global Technologies Pvt Ltd
            Email: alex.hamilton@apextech.io
            Mobile: +1 (555) 234-5678
            Website: www.apextech.io
            100 Wall Street, Floor 24, New York, NY 10005
        """.trimIndent()

        val contact = BusinessCardParser.parse(ocrText)

        assertTrue(contact.hasData)
        assertEquals("Alexander Hamilton", contact.fullName)
        assertEquals("Senior Software Architect", contact.jobTitle)
        assertEquals("Apex Global Technologies Pvt Ltd", contact.company)
        assertEquals("alex.hamilton@apextech.io", contact.email)
        assertNotNull(contact.phone)
        assertTrue(contact.phone!!.contains("234-5678"))
        assertNotNull(contact.website)
        assertTrue(contact.website!!.contains("apextech.io"))
        assertNotNull(contact.address)
        assertTrue(contact.address!!.contains("Wall Street"))
    }

    @Test
    fun parse_ceoCard_extractsTitleAndCompany() {
        val ocrText = """
            Elena Rostova
            Chief Executive Officer
            Quantum Cyber Solutions Inc
            elena@quantumcyber.com
            Tel: +44 20 7946 0991
        """.trimIndent()

        val contact = BusinessCardParser.parse(ocrText)

        assertTrue(contact.hasData)
        assertEquals("Elena Rostova", contact.fullName)
        assertEquals("Chief Executive Officer", contact.jobTitle)
        assertEquals("Quantum Cyber Solutions Inc", contact.company)
        assertEquals("elena@quantumcyber.com", contact.email)
    }

    @Test
    fun toVCardString_formatsValidVCard30() {
        val contact = ExtractedContactData(
            fullName = "John Doe",
            jobTitle = "Software Engineer",
            company = "Acme Corp",
            phone = "+1 234 567 8900",
            email = "john@example.com",
            website = "example.com",
            address = "123 Main St, Springfield"
        )

        val vcf = contact.toVCardString()

        assertTrue(vcf.startsWith("BEGIN:VCARD"))
        assertTrue(vcf.contains("VERSION:3.0"))
        assertTrue(vcf.contains("FN:John Doe"))
        assertTrue(vcf.contains("N:Doe;John;;;"))
        assertTrue(vcf.contains("ORG:Acme Corp"))
        assertTrue(vcf.contains("TITLE:Software Engineer"))
        assertTrue(vcf.contains("TEL;TYPE=CELL,VOICE:+1 234 567 8900"))
        assertTrue(vcf.contains("EMAIL;TYPE=INTERNET,PREF:john@example.com"))
        assertTrue(vcf.contains("URL:https://example.com"))
        assertTrue(vcf.contains("ADR;TYPE=WORK:;;123 Main St, Springfield;;;;"))
        assertTrue(vcf.trim().endsWith("END:VCARD"))
    }
}
