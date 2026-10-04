package com.example.elenchos.testing.runtime

object InputFuzzer {

    data class FuzzVector(
        val category: String,
        val value: String,
        val description: String
    )

    val FUZZ_VECTORS = listOf(
        FuzzVector("EMPTY", "", "Empty string (zero-length input validation)"),
        FuzzVector("WHITESPACE", "     ", "Whitespace-only characters"),
        FuzzVector("LONG_TEXT_500", "A".repeat(500), "500-character boundary string buffer stress"),
        FuzzVector("LONG_TEXT_2000", "X".repeat(2000), "2000-character extreme length buffer overflow check"),
        FuzzVector("UNICODE_EMOJI", "🚀🧪🔥💻⚡🎉🤖💥💡🎯", "Multi-byte UTF-8 emoji string handling"),
        FuzzVector("SPECIAL_CHARS", "!@#\$%^&*()_+-=[]{}|;':\",./<>?", "High-entropy punctuation and symbol set"),
        FuzzVector("SQL_INJECTION_MOCK", "' OR '1'='1' -- ", "SQL quote escaping robustness check"),
        FuzzVector("PATH_TRAVERSAL_MOCK", "../../../../../etc/hosts", "Path traversal boundary sequence"),
        FuzzVector("XSS_MOCK", "<script>alert('elenchos')</script>", "HTML/XML script tag parsing safety"),
        FuzzVector("NUMERIC_ZERO", "0", "Zero numeric boundary"),
        FuzzVector("NUMERIC_NEGATIVE", "-1", "Negative integer boundary"),
        FuzzVector("NUMERIC_OVERFLOW", "999999999999999999", "Large integer 64-bit overflow string"),
        FuzzVector("NUMERIC_DECIMAL", "3.14159265358979323846", "Floating point high-precision decimal"),
        FuzzVector("MALFORMED_EMAIL", "developer@@domain..com", "Malformed email structure for validator crash check"),
        FuzzVector("MALFORMED_URL", "htt://localhost:8080/invalid", "Malformed URI scheme for parsing resilience")
    )

    fun getRandomFuzzVector(): FuzzVector = FUZZ_VECTORS.random()
}
