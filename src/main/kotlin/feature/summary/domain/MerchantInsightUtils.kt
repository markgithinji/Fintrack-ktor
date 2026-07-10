package feature.summary.domain

object MerchantInsightUtils {

    fun isDescriptionMeaningful(description: String?, categoryName: String): Boolean {
        if (description.isNullOrBlank()) return false
        val cleaned = cleanMerchantName(description)

        if (cleaned.length < 3) return false

        if (cleaned.all { it.isDigit() || it == '-' || it == '.' || it == ':' || it == '#' || it == '/' || it == ' ' }) return false

        val normalizedCategory = categoryName.lowercase().replace(" ", "").replace("-", "")
        val normalizedDesc = cleaned.lowercase().replace(" ", "").replace("-", "")

        if (normalizedDesc == normalizedCategory) return false

        if (normalizedDesc.contains(normalizedCategory) || normalizedCategory.contains(normalizedDesc)) {
            val lengthDiff = kotlin.math.abs(normalizedDesc.length - normalizedCategory.length)
            if (lengthDiff <= 3) return false
        }

        return true
    }

    fun cleanMerchantName(description: String): String {
        // 1. Remove common reference labels like "Ref:", "Reference:", etc.
        var cleaned = description.replace(Regex("(?i)\\b(Ref|Reference|Ref No|Ref#|Ref ID)[:.]?\\s*"), "").trim()

        // 2. Remove transaction codes (e.g., OAG8123456), potentially in parentheses
        cleaned = cleaned.replace(Regex("\\(?\\s*[A-Z0-9]{10}\\s*\\)?"), "").trim()
        
        // 3. Remove common boilerplate words
        val noiseWords = listOf("Confirmed", "Ksh", "Paid to", "Sent to", "on", "at")
        noiseWords.forEach { word ->
            cleaned = cleaned.replace(Regex("(?i)\\b$word\\b"), "")
        }

        // 4. Remove trailing numbers/IDs (e.g., UBER *1234 -> UBER)
        cleaned = cleaned.split("*", "#", " - ").first().trim()

        // 5. Final numeric cleanup (remove 4+ digit codes)
        cleaned = cleaned.replace(Regex("[0-9]{4,}"), "").trim()

        // 6. Remove empty parentheses ()
        cleaned = cleaned.replace(Regex("\\(\\s*\\)"), "").trim()

        // 7. Final strip of leading/trailing parentheses if they wrap the name
        return cleaned.removePrefix("(").removeSuffix(")").trim()
    }
}
