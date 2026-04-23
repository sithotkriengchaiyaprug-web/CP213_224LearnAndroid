package com.example.zerotouchbudget

import com.example.zerotouchbudget.domain.model.ReceiptAutoScanHeuristics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptAutoScanHeuristicsTest {

    private val heuristics = ReceiptAutoScanHeuristics()

    @Test
    fun `bank folder and filename keywords are detected`() {
        assertTrue(heuristics.isBankFolder("Pictures/SCB/"))
        assertTrue(heuristics.isLikelyReceiptFile("transfer_slip_001.jpg"))
        assertFalse(heuristics.isBankFolder("Pictures/Downloads/"))
        assertFalse(heuristics.isLikelyReceiptFile("holiday_photo.jpg"))
    }

    @Test
    fun `ocr keywords are detected across thai and english`() {
        assertTrue(heuristics.containsReceiptKeywords("ยอดชำระเงิน จำนวนเงิน 123.45 บาท"))
        assertTrue(heuristics.containsReceiptKeywords("Total Amount 150.00"))
        assertFalse(heuristics.containsReceiptKeywords("This is a landscape photo"))
    }

    @Test
    fun `primary folder is selected from highest learned count`() {
        val primary = heuristics.selectPrimaryFolder(
            mapOf(
                "SCB" to 2,
                "KBank" to 8,
                "Krungthai" to 3
            )
        )

        assertEquals("KBank", primary)
    }

    @Test
    fun `candidate score favors bank folders and receipt-like names`() {
        val score = heuristics.scoreCandidate(
            relativePath = "Pictures/SCB/",
            displayName = "transfer_slip_001.jpg",
            ocrText = "Amount 100.00"
        )

        assertTrue(score > 100)
    }
}
