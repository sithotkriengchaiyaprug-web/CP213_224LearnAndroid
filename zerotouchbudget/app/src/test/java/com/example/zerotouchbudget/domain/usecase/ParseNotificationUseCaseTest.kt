package com.example.zerotouchbudget.domain.usecase

import com.example.zerotouchbudget.domain.repository.TransactionRepository
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ParseNotificationUseCaseTest {

    private val repository: TransactionRepository = mockk(relaxed = true)
    private val useCase = ParseNotificationUseCase(repository)

    @Test
    fun `extractAmount - should pick the largest number when OTP is present`() {
        val text = "ยอดชำระ 1,250.00 บาท รหัส OTP ของคุณคือ 987654"
        // เราคาดหวังว่ามันจะเอา 1250 ไม่ใช่ 987654 (เพราะเราจำกัด range ไว้ที่ 100,000 ในโค้ด)
        // หมายเหตุ: ในโค้ดจริงเราเช็ค value in 1.0..100000.0
        val amount = invokePrivateExtractAmount(text)
        assertEquals(1250.0, amount, 0.01)
    }

    @Test
    fun `extractAmount - should handle Thai bank format`() {
        val text = "รายการโอนเงิน จำนวนเงิน 500.00 บาท ให้ นายสมชาย"
        val amount = invokePrivateExtractAmount(text)
        assertEquals(500.0, amount, 0.01)
    }

    @Test
    fun `extractAmount - should handle English format with THB`() {
        val text = "Paid 150.75 THB at Starbucks"
        val amount = invokePrivateExtractAmount(text)
        assertEquals(150.75, amount, 0.01)
    }

    @Test
    fun `extractBrand - should extract merchant name correctly`() {
        val text = "ชำระเงินให้ ร้านข้าวมันไก่ตอน จำนวน 60 บาท"
        val brand = invokePrivateExtractBrand(text, "K-Plus")
        assertEquals("ร้านข้าวมันไก่ตอน", brand)
    }

    @Test
    fun `mapCategory - should map Starbucks to Drinks`() {
        val category = invokePrivateMapCategory("Starbucks Coffee", "")
        assertEquals("Drinks", category)
    }

    @Test
    fun `mapCategory - should map MRT to Transport`() {
        val category = invokePrivateMapCategory("MRT Blue Line", "")
        assertEquals("Transport", category)
    }

    // Helper functions to test private methods using reflection
    private fun invokePrivateExtractAmount(text: String): Double? {
        val method = useCase.javaClass.getDeclaredMethod("extractAmount", String::class.java)
        method.isAccessible = true
        return method.invoke(useCase, text) as Double?
    }

    private fun invokePrivateExtractBrand(text: String, title: String): String {
        val method = useCase.javaClass.getDeclaredMethod("extractBrand", String::class.java, String::class.java)
        method.isAccessible = true
        return method.invoke(useCase, text, title) as String
    }

    private fun invokePrivateMapCategory(brand: String, text: String): String {
        val method = useCase.javaClass.getDeclaredMethod("mapCategory", String::class.java, String::class.java)
        method.isAccessible = true
        return method.invoke(useCase, brand, text) as String
    }
}
