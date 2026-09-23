package com.example.docscanner.service

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class PdfAnnotationAndSignatureTest {

    private fun createSamplePdf(): ByteArray {
        val stream = ByteArrayOutputStream()
        val writer = PdfWriter(stream)
        val pdf = PdfDocument(writer)
        val doc = Document(pdf)
        doc.add(Paragraph("Sample Page 1 Content"))
        pdf.addNewPage()
        doc.add(Paragraph("Sample Page 2 Content"))
        doc.close()
        pdf.close()
        return stream.toByteArray()
    }

    private fun create1x1PngBytes(): ByteArray {
        // Minimal valid 1x1 transparent PNG bytes
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
