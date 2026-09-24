package com.example.docscanner.service

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class PdfAnnotationAndSignatureTest {

    private fun createSamplePdf(): ByteArray {
        val stream = ByteArrayOutputStream()
        val document = PDDocument()

        val page1 = PDPage()
        document.addPage(page1)
        val stream1 = PDPageContentStream(document, page1)
        stream1.beginText()
        stream1.setFont(PDType1Font.HELVETICA, 12f)
        stream1.newLineAtOffset(50f, 700f)
        stream1.showText("Sample Page 1 Content")
        stream1.endText()
        stream1.close()

        val page2 = PDPage()
        document.addPage(page2)
        val stream2 = PDPageContentStream(document, page2)
        stream2.beginText()
        stream2.setFont(PDType1Font.HELVETICA, 12f)
        stream2.newLineAtOffset(50f, 700f)
        stream2.showText("Sample Page 2 Content")
        stream2.endText()
        stream2.close()

        document.save(stream)
        document.close()
        return stream.toByteArray()
    }

    private fun create1x1PngBytes(): ByteArray {
        return byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte(),
            0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x08.toByte(), 0x06.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x1F.toByte(), 0x15.toByte(), 0xC4.toByte(), 0x89.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0A.toByte(),
            0x49.toByte(), 0x44.toByte(), 0x41.toByte(), 0x54.toByte(),
            0x78.toByte(), 0x9C.toByte(), 0x63.toByte(), 0x00.toByte(),
            0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x05.toByte(),
            0x00.toByte(), 0x01.toByte(), 0x0D.toByte(), 0x0A.toByte(),
            0x2D.toByte(), 0xB4.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x49.toByte(), 0x45.toByte(),
            0x4E.toByte(), 0x44.toByte(), 0xAE.toByte(), 0x42.toByte(),
            0x60.toByte(), 0x82.toByte()
        )
    }

    @Test
    fun testStampWatermark() {
        val originalPdf = createSamplePdf()
        assertTrue(originalPdf.isNotEmpty())

        val config = StampConfig(
            text = "CONFIDENTIAL",
            colorHex = "#D32F2F",
            position = StampPosition.CENTER_WATERMARK,
            opacity = 0.4f
        )
        val stampedPdf = PdfAnnotationService.stampPdf(originalPdf, config)

        assertTrue("Stamped PDF should not be empty", stampedPdf.isNotEmpty())
        assertFalse("Stamped PDF should differ from original", stampedPdf.contentEquals(originalPdf))
        assertTrue("Stamped PDF should be larger than original", stampedPdf.size > originalPdf.size)
    }

    @Test
    fun testStampSignature() {
        val originalPdf = createSamplePdf()
        val png = create1x1PngBytes()

        val signedPdf = SignatureService.stampSignature(
            pdfBytes = originalPdf,
            signaturePngBytes = png,
            targetPages = listOf(1),
            placement = SignaturePlacement.BOTTOM_RIGHT
        )

        assertTrue("Signed PDF should not be empty", signedPdf.isNotEmpty())
        assertFalse("Signed PDF should differ from original", signedPdf.contentEquals(originalPdf))
        assertTrue("Signed PDF should be larger than original", signedPdf.size > originalPdf.size)
    }

    @Test
    fun testStampHeaderAndFooterPositions() {
        val originalPdf = createSamplePdf()

        val headerConfig = StampConfig(
            text = "APPROVED",
            colorHex = "#388E3C",
            position = StampPosition.TOP_HEADER,
            opacity = 0.5f
        )
        val headerPdf = PdfAnnotationService.stampPdf(originalPdf, headerConfig)
        assertTrue(headerPdf.isNotEmpty())
        assertFalse(headerPdf.contentEquals(originalPdf))

        val footerConfig = StampConfig(
            text = "VERIFIED",
            colorHex = "#1976D2",
            position = StampPosition.BOTTOM_FOOTER,
            opacity = 0.5f
        )
        val footerPdf = PdfAnnotationService.stampPdf(originalPdf, footerConfig)
        assertTrue(footerPdf.isNotEmpty())
        assertFalse(footerPdf.contentEquals(originalPdf))
    }

    @Test
    fun testStampSignaturePlacements() {
        val originalPdf = createSamplePdf()
        val png = create1x1PngBytes()

        for (placement in SignaturePlacement.entries) {
            val signedPdf = SignatureService.stampSignature(
                pdfBytes = originalPdf,
                signaturePngBytes = png,
                targetPages = listOf(1),
                placement = placement
            )
            assertTrue("PDF stamped with $placement should not be empty", signedPdf.isNotEmpty())
            assertFalse("PDF stamped with $placement should differ from original", signedPdf.contentEquals(originalPdf))
        }
    }
}
