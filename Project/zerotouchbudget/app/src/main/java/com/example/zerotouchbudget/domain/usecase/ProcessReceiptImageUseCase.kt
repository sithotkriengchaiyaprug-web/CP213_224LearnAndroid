package com.example.zerotouchbudget.domain.usecase
 
import android.graphics.Bitmap
import android.util.Log
import com.example.zerotouchbudget.domain.model.Transaction
import com.example.zerotouchbudget.domain.model.TransactionSource
import com.example.zerotouchbudget.domain.repository.TransactionRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull
 
/**
 * Fixed version of ProcessReceiptImageUseCase
 * 
 * Key improvements:
 * 1. ✅ Enhanced prompt with clear JSON format requirement
 * 2. ✅ Better image quality (1536px instead of 1024px)
 * 3. ✅ Robust JSON parsing with fallback strategies
 * 4. ✅ Proper error handling and detailed logging
 * 5. ✅ Type-safe value extraction
 * 6. ✅ API timeout protection
 */
class ProcessReceiptImageUseCase @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val repository: TransactionRepository
) {
    
    suspend operator fun invoke(bitmap: Bitmap?): Result<Transaction> = runCatching {
        if (bitmap == null) throw Exception("Bitmap is null")
        
        Log.d(TAG, "=== Starting receipt OCR ===")
        Log.d(TAG, "Input image: ${bitmap.width}x${bitmap.height}px, ${(bitmap.byteCount / 1024)}KB")
        
        // Step 1: Resize image for better quality
        val resizedBitmap = resizeBitmap(bitmap, maxWidth = 1536)
        Log.d(TAG, "Resized to: ${resizedBitmap.width}x${resizedBitmap.height}px")
        
        // Step 2: Build optimized prompt
        val prompt = buildOptimizedPrompt()
        
        // Step 3: Call Gemini API with timeout
        val response = callGeminiWithTimeout(resizedBitmap, prompt)
        val rawText = response.text ?: throw Exception("Empty response from Gemini API")
        
        Log.d(TAG, "Raw API Response:\n$rawText")
        
        // Step 4: Parse JSON safely
        val json = parseJsonSafely(rawText)
        Log.d(TAG, "Parsed JSON: $json")
        
        // Step 5: Extract and validate data
        val (amount, brand, confidence) = extractReceiptData(json)
        Log.d(TAG, "Extracted: amount=$amount, brand=$brand, confidence=$confidence")
        
        // Step 6: Validate result
        validateExtraction(amount, brand)
        
        // Step 7: Create transaction
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            brand = brand,
            category = mapCategory(brand),
            timestamp = System.currentTimeMillis(),
            source = TransactionSource.OCR,
            note = "Scanned via Gemini AI (confidence: $confidence)"
        )
        
        repository.insertTransaction(transaction)
        Log.d(TAG, "✅ Transaction saved: ID=${transaction.id}, Amount=$amount, Brand=$brand")
        
        transaction
    }
    
    /**
     * Build an optimized prompt that forces JSON response
     * และให้ Gemini สำคัญกับข้อมูลที่ถูกต้อง
     */
    private fun buildOptimizedPrompt(): String = """
        You are a professional receipt and bank slip OCR system.
        Your task is to extract transaction data from receipt/bank slip images.
        
        IMPORTANT RULES:
        1. You MUST respond ONLY with valid JSON - absolutely no explanations, markdown, or extra text
        2. Return EXACTLY this JSON structure (no additional fields):
           {"amount": number, "brand": "string", "confidence": "high|medium|low"}
        
        3. For AMOUNT extraction:
           - Find the FINAL TOTAL amount (not subtotal, not individual items)
           - Look for keywords: "Total", "Amount Due", "รวม", "ทั้งสิ้น", "ราคารวม"
           - Accept formats: 1234.56, 1,234.56, 1234บาท, 1234฿
           - If you cannot find amount, use 0.0 (don't throw error)
        
        4. For BRAND extraction:
           - Find store/merchant/bank name from logo or text
           - Accept: store names, restaurant names, bank names, payment gateway names
           - Look at: header text, logos, bill-to information, merchant name field
           - Common Thai brands: Starbucks, Central, True Money, Bangkok Bank, etc.
           - If you cannot identify brand, use "Unknown"
        
        5. For CONFIDENCE:
           - "high" = clearly visible amount and merchant name
           - "medium" = amount visible but brand unclear, or vice versa
           - "low" = poor image quality or both fields unclear
        
        RESPOND ONLY WITH THIS JSON (no code blocks, no backticks):
        {"amount": 0.0, "brand": "Unknown", "confidence": "low"}
    """.trimIndent()
    
    /**
     * Call Gemini API with 30-second timeout
     */
    private suspend fun callGeminiWithTimeout(
        bitmap: Bitmap,
        prompt: String
    ) = withTimeoutOrNull(30000L) {
        Log.d(TAG, "Calling Gemini API...")
        generativeModel.generateContent(content {
            image(bitmap)
            text(prompt)
        })
    } ?: throw Exception("Gemini API timeout after 30 seconds")
    
    /**
     * Parse JSON with multiple fallback strategies
     * เหตุผล: Gemini บางครั้งตอบมา:
     * - "```json { ... } ```" (with markdown)
     * - Plain text แทนที่จะเป็น JSON
     * - JSON nested ข้างใน text อื่น
     */
    private fun parseJsonSafely(rawText: String): JSONObject {
        var cleaned = rawText.trim()
        
        Log.d(TAG, "Attempting JSON parse. Length=${cleaned.length}")
        
        // Strategy 1: Remove markdown code blocks
        cleaned = cleaned
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        
        Log.d(TAG, "After markdown cleanup: $cleaned")
        
        // Strategy 2: Try direct parse
        try {
            return JSONObject(cleaned)
        } catch (e: JSONException) {
            Log.d(TAG, "Direct parse failed: ${e.message}")
        }
        
        // Strategy 3: Find JSON substring using proper bracket matching
        val jsonStr = extractJsonSubstring(cleaned)
        if (jsonStr.isNotEmpty()) {
            try {
                Log.d(TAG, "Extracted JSON substring: $jsonStr")
                return JSONObject(jsonStr)
            } catch (e: JSONException) {
                Log.d(TAG, "Substring parse failed: ${e.message}")
            }
        }
        
        // Strategy 4: If all else fails, return safe default
        Log.e(TAG, "All JSON parse strategies failed. Using default.")
        return JSONObject().apply {
            put("amount", 0.0)
            put("brand", "Unknown")
            put("confidence", "low")
        }
    }
    
    /**
     * Extract JSON substring using proper { } bracket matching
     * แทนที่จะใช้ greedy regex ที่ผิด
     */
    private fun extractJsonSubstring(text: String): String {
        val startIdx = text.indexOf('{')
        if (startIdx == -1) {
            Log.d(TAG, "No '{' found in text")
            return ""
        }
        
        var braceCount = 0
        var inString = false
        var escapeNext = false
        
        for (i in startIdx until text.length) {
            val char = text[i]
            
            // Handle escape sequences
            if (escapeNext) {
                escapeNext = false
                continue
            }
            
            if (char == '\\') {
                escapeNext = true
                continue
            }
            
            // Track string boundaries
            if (char == '"' && !inString) {
                inString = true
                continue
            } else if (char == '"' && inString) {
                inString = false
                continue
            }
            
            // Count braces only outside strings
            if (!inString) {
                when (char) {
                    '{' -> braceCount++
                    '}' -> {
                        braceCount--
                        if (braceCount == 0) {
                            return text.substring(startIdx, i + 1)
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "Incomplete JSON: unclosed braces")
        return text.substring(startIdx)
    }
    
    /**
     * Extract amount, brand, confidence from JSON
     * ด้วยการรองรับประเภท (string/number/null) แบบปลอดภัย
     */
    private fun extractReceiptData(json: JSONObject): Triple<Double, String, String> {
        // Extract amount (handle string, int, double, null)
        var amount = 0.0
        if (json.has("amount")) {
            try {
                val amountVal = json.get("amount")
                amount = when (amountVal) {
                    is Number -> amountVal.toDouble()
                    is String -> {
                        // Clean string: remove commas, ฿, บาท, etc.
                        val cleaned = amountVal
                            .replace(",", "")
                            .replace("฿", "")
                            .replace("บาท", "")
                            .trim()
                        cleaned.toDoubleOrNull() ?: 0.0
                    }
                    JSONObject.NULL -> 0.0
                    else -> 0.0
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse amount: ${e.message}")
                amount = 0.0
            }
        }
        
        // Extract brand (handle null, empty)
        val brand = json.optString("brand", "").trim().ifEmpty { "Unknown" }
        
        // Extract confidence
        val confidence = json.optString("confidence", "low")
        
        return Triple(amount, brand, confidence)
    }
    
    /**
     * Validate that we extracted meaningful data
     */
    private fun validateExtraction(amount: Double, brand: String) {
        // Allow amount=0 if brand is identified (e.g., QR code payment)
        // But reject if both are empty
        if (amount <= 0.0 && (brand.isEmpty() || brand == "Unknown")) {
            throw Exception(
                "OCR failed: No valid amount or brand extracted. " +
                "Receipt may be unclear or unsupported format."
            )
        }
        
        // Warn if confidence is low
        if (amount <= 0.0) {
            Log.w(TAG, "Warning: Amount is 0 - receipt may not contain transaction value")
        }
    }
    
    /**
     * Resize bitmap untuk optimal Gemini performance
     * ปรับเพิ่มเป็น 1536px (from 1024) เพื่อให้ข้อความเล็กๆ อ่านได้ชัด
     */
    private fun resizeBitmap(source: Bitmap, maxWidth: Int = 1536): Bitmap {
        // ถ้าภาพเล็กกว่า maxWidth อยู่แล้ว ไม่ต้อง resize
        if (source.width <= maxWidth) return source
        
        val aspectRatio = source.height.toDouble() / source.width.toDouble()
        val targetHeight = (maxWidth * aspectRatio).toInt()
        
        Log.d(TAG, "Resizing: ${source.width}x${source.height} → ${maxWidth}x$targetHeight")
        
        return Bitmap.createScaledBitmap(source, maxWidth, targetHeight, true)
    }
    
    /**
     * Map brand name to transaction category
     * เหมือนเดิม แต่เพิ่มเติม
     */
    private fun mapCategory(brand: String): String {
        val b = brand.lowercase()
        return when {
            // Drinks & Coffee
            listOf("starbucks", "cafe", "coffee", "กาแฟ").any { b.contains(it) } -> "Drinks"
            listOf("amazon", "แอมะ", "amazon prime").any { b.contains(it) } -> "Online Shopping"
            
            // Groceries
            listOf("7-11", "seven", "lotus", "tops", "bigc", "tesco").any { b.contains(it) } -> "Groceries"
            listOf("เซเวน", "โลตัส", "ท็อปส์", "บิ๊กซี").any { b.contains(it) } -> "Groceries"
            
            // Food Delivery
            listOf("grab", "lineman", "foodpanda", "shopee").any { b.contains(it) } -> "Food Delivery"
            listOf("แกรป", "ไลนแมน", "ฟู้ดแพนดา").any { b.contains(it) } -> "Food Delivery"
            
            // Transport
            listOf("shell", "ptt", "esso", "bts", "mrt", "grab taxi").any { b.contains(it) } -> "Transport"
            listOf("พีทีที", "บีทีเอส", "เอมอร์ที").any { b.contains(it) } -> "Transport"
            
            // Banking & Payments
            listOf("bangkok bank", "kasikornbank", "krungsri", "true money", "omise").any { b.contains(it) } -> "Finance"
            
            // Utilities
            listOf("mea", "met", "true", "dtac").any { b.contains(it) } -> "Utilities"
            
            else -> "General"
        }
    }
    
    companion object {
        private const val TAG = "ProcessReceiptOCR"
    }
}