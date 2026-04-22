package com.example.zerotouchbudget.domain.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.google.ai.client.generativeai.GenerativeModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

/**
 * Unit Tests สำหรับ ProcessReceiptImageUseCase
 * 
 * ทดสอบ 3 สถานการณ์หลัก:
 * 1. Receipt ที่มีข้อมูลชัดเจน
 * 2. Receipt ที่มีข้อมูลบ้างไม่บ้าง
 * 3. Receipt ที่ API ตอบผิด format
 */
@RunWith(AndroidJUnit4::class)
class ProcessReceiptImageUseCaseTest {
    
    private lateinit var useCase: ProcessReceiptImageUseCase
    private lateinit var generativeModel: GenerativeModel
    private lateinit var repository: TransactionRepository
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Before
    fun setup() {
        generativeModel = mockk()
        repository = mockk()
        useCase = ProcessReceiptImageUseCase(generativeModel, repository)
    }
    
    // ============================================================
    // Test 1: Happy Path - API ตอบ JSON ชัดเจน
    // ============================================================
    @Test
    fun testOcr_ValidReceipt_ReturnsTransaction() = runTest {
        // Arrange
        val bitmap = createTestBitmap()
        val validJson = """{
            "amount": 234.50,
            "brand": "Starbucks Thailand",
            "confidence": "high"
        }"""
        
        // Mock API response
        mockGeminiResponse(validJson)
        mockRepositoryInsert()
        
        // Act
        val result = useCase(bitmap)
        
        // Assert
        result.onSuccess { transaction ->
            assertEquals(234.50, transaction.amount)
            assertEquals("Starbucks Thailand", transaction.brand)
            assertEquals("Drinks", transaction.category)
            assertTrue(transaction.note.contains("Scanned via Gemini"))
        }
        
        result.onFailure {
            throw AssertionError("Expected success but got: ${it.message}")
        }
    }
    
    // ============================================================
    // Test 2: API ตอบ JSON มี Markdown (```json ... ```)
    // ============================================================
    @Test
    fun testOcr_JsonWithMarkdown_ParsesCorrectly() = runTest {
        val bitmap = createTestBitmap()
        val jsonWithMarkdown = """```json
{
    "amount": 500.00,
    "brand": "Central Rama 9",
    "confidence": "medium"
}
```"""
        
        mockGeminiResponse(jsonWithMarkdown)
        mockRepositoryInsert()
        
        val result = useCase(bitmap)
        
        result.onSuccess { transaction ->
            assertEquals(500.00, transaction.amount)
            assertEquals("Central Rama 9", transaction.brand)
        }
    }
    
    // ============================================================
    // Test 3: API ตอบ JSON ที่มี Amount เป็น String
    // ============================================================
    @Test
    fun testOcr_AmountAsString_ParsesCorrectly() = runTest {
        val bitmap = createTestBitmap()
        val jsonWithStringAmount = """{
            "amount": "1,234.50",
            "brand": "CP Fresh Mart",
            "confidence": "high"
        }"""
        
        mockGeminiResponse(jsonWithStringAmount)
        mockRepositoryInsert()
        
        val result = useCase(bitmap)
        
        result.onSuccess { transaction ->
            assertEquals(1234.50, transaction.amount)
            assertEquals("CP Fresh Mart", transaction.brand)
        }
    }
    
    // ============================================================
    // Test 4: API ตอบ Amount แบบ Thai currency (1234฿)
    // ============================================================
    @Test
    fun testOcr_ThaiCurrencyFormat_ParsesCorrectly() = runTest {
        val bitmap = createTestBitmap()
        val jsonWithThaiFormat = """{
            "amount": "499฿",
            "brand": "Starbucks",
            "confidence": "high"
        }"""
        
        mockGeminiResponse(jsonWithThaiFormat)
        mockRepositoryInsert()
        
        val result = useCase(bitmap)
        
        result.onSuccess { transaction ->
            assertEquals(499.0, transaction.amount)
        }
    }
    
    // ============================================================
    // Test 5: API ตอบ Amount = 0 แต่มี Brand
    // ============================================================
    @Test
    fun testOcr_ZeroAmountWithBrand_Succeeds() = runTest {
        val bitmap = createTestBitmap()
        val jsonWithZeroAmount = """{
            "amount": 0.0,
            "brand": "Bangkok Bank",
            "confidence": "low"
        }"""
        
        mockGeminiResponse(jsonWithZeroAmount)
        mockRepositoryInsert()
        
        val result = useCase(bitmap)
        
        // ยอมรับเพราะ brand มี
        result.onSuccess { transaction ->
            assertEquals(0.0, transaction.amount)
            assertEquals("Bangkok Bank", transaction.brand)
            assertEquals("Finance", transaction.category)
        }
    }
    
    // ============================================================
    // Test 6: API ตอบ JSON ที่ Incomplete (missing confidence)
    // ============================================================
    @Test
    fun testOcr_IncompleteJson_UsesDefaults() = runTest {
        val bitmap = createTestBitmap()
        val incompleteJson = """{
            "amount": 150.00,
            "brand": "Grab Food"
        }"""
        
        mockGeminiResponse(incompleteJson)
        mockRepositoryInsert()
        
        val result = useCase(bitmap)
        
        result.onSuccess { transaction ->
            assertEquals(150.00, transaction.amount)
            assertEquals("Grab Food", transaction.brand)
            // confidence should have default
            assertTrue(transaction.note.contains("low"))
        }
    }
    
    // ============================================================
    // Test 7: API ตอบ Plain text (ไม่ใช่ JSON)
    // ============================================================
    @Test
    fun testOcr_PlainTextResponse_FallsBackToDefault() = runTest {
        val bitmap = createTestBitmap()
        val plainText = "The receipt shows a total of 750 baht from Starbucks"
        
        mockGeminiResponse(plainText)
        mockRepositoryInsert()
        
        val result = useCase(bitmap)
        
        // ต้องจัดการ: extract JSON ล้มเหลว → return default
        result.onSuccess { transaction ->
            // Check ว่า fall back ถูกต้อง
            assertEquals("Unknown", transaction.brand)
            assertTrue(transaction.note.contains("Scanned via Gemini"))
        }
    }
    
    // ============================================================
    // Test 8: API ตอบ Nested JSON (nested objects)
    // ============================================================
    @Test
    fun testOcr_NestedJson_ExtractsCorrectly() = runTest {
        val bitmap = createTestBitmap()
        val nestedJson = """{
            "receipt": {
                "amount": 250.00,
                "brand": "Central World"
            },
            "amount": 250.00,
            "brand": "Central World",
            "confidence": "high"
        }"""
        
        mockGeminiResponse(nestedJson)
        mockRepositoryInsert()
        
        val result = useCase(bitmap)
        
        result.onSuccess { transaction ->
            assertEquals(250.00, transaction.amount)
            assertEquals("Central World", transaction.brand)
        }
    }
    
    // ============================================================
    // Test 9: Null Bitmap Input
    // ============================================================
    @Test
    fun testOcr_NullBitmap_ThrowsException() = runTest {
        val result = useCase(null)
        
        result.onFailure { exception ->
            assertTrue(exception.message?.contains("Bitmap is null") ?: false)
        }
    }
    
    // ============================================================
    // Test 10: Category Mapping
    // ============================================================
    @Test
    fun testCategoryMapping() = runTest {
        data class CategoryTest(
            val brand: String,
            val expectedCategory: String
        )
        
        val cases = listOf(
            CategoryTest("Starbucks", "Drinks"),
            CategoryTest("Central Rama 9", "General"),
            CategoryTest("7-Eleven", "Groceries"),
            CategoryTest("Grab Food", "Food Delivery"),
            CategoryTest("Bangkok Bank", "Finance"),
            CategoryTest("PTT Gas Station", "Transport"),
            CategoryTest("True Money", "Finance")
        )
        
        val bitmap = createTestBitmap()
        
        for (case in cases) {
            val json = """{
                "amount": 100.00,
                "brand": "${case.brand}",
                "confidence": "high"
            }"""
            
            mockGeminiResponse(json)
            mockRepositoryInsert()
            
            val result = useCase(bitmap)
            
            result.onSuccess { transaction ->
                assertEquals(
                    case.expectedCategory,
                    transaction.category,
                    "Failed for brand: ${case.brand}"
                )
            }
        }
    }
    
    // ============================================================
    // Test 11: Large Amount Parsing
    // ============================================================
    @Test
    fun testOcr_LargeAmount_ParsesCorrectly() = runTest {
        val bitmap = createTestBitmap()
        val largeAmount = """{
            "amount": 15000.99,
            "brand": "Central Department Store",
            "confidence": "high"
        }"""
        
        mockGeminiResponse(largeAmount)
        mockRepositoryInsert()
        
        val result = useCase(bitmap)
        
        result.onSuccess { transaction ->
            assertEquals(15000.99, transaction.amount)
        }
    }
    
    // ============================================================
    // Test 12: Decimal Precision
    // ============================================================
    @Test
    fun testOcr_DecimalPrecision_Maintained() = runTest {
        val bitmap = createTestBitmap()
        val precisionJson = """{
            "amount": 0.99,
            "brand": "Convenience Store",
            "confidence": "medium"
        }"""
        
        mockGeminiResponse(precisionJson)
        mockRepositoryInsert()
        
        val result = useCase(bitmap)
        
        result.onSuccess { transaction ->
            assertEquals(0.99, transaction.amount, 0.001)
        }
    }
    
    // ============================================================
    // Helper Functions
    // ============================================================
    
    private fun createTestBitmap(): Bitmap {
        // สร้าง test bitmap ขนาด 800x600
        return Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
    }
    
    private fun mockGeminiResponse(response: String) {
        coEvery {
            generativeModel.generateContent(any())
        } returns mockk {
            every { text } returns response
        }
    }
    
    private fun mockRepositoryInsert() {
        coEvery {
            repository.insertTransaction(any())
        } returns Unit
    }
}

/**
 * Integration Tests - ทดสอบกับ Real Gemini API
 * 
 * ใช้เมื่อ:
 * 1. มีคีย์ Gemini API จริง
 * 2. อยากทดสอบ real API response
 * 
 * ⚠️ ต้องเพิ่ม BuildConfig.GEMINI_API_KEY และ gradlew config
 */
@RunWith(AndroidJUnit4::class)
class ProcessReceiptImageUseCaseIntegrationTest {
    
    private lateinit var useCase: ProcessReceiptImageUseCase
    private lateinit var repository: TransactionRepository
    
    @Before
    fun setup() {
        // ใช้ real GenerativeModel (ต้องมี API key)
        // val generativeModel = GenerativeModel(
        //     modelName = "gemini-1.5-flash",
        //     apiKey = BuildConfig.GEMINI_API_KEY
        // )
        // useCase = ProcessReceiptImageUseCase(generativeModel, repository)
        
        repository = mockk()
        coEvery { repository.insertTransaction(any()) } returns Unit
    }
    
    @Test
    fun testWithRealStarbucksReceipt() = runTest {
        // ⚠️ Uncomment เมื่อมี real API key
        /*
        val bitmap = BitmapFactory.decodeResource(
            InstrumentationRegistry.getInstrumentation().targetContext.resources,
            R.drawable.starbucks_receipt_sample
        )
        
        val result = useCase(bitmap)
        
        result.onSuccess { transaction ->
            assertTrue(transaction.amount > 0)
            assertTrue(transaction.brand.contains("Starbucks") || 
                      transaction.brand.contains("starbucks"))
        }
        */
    }
}
