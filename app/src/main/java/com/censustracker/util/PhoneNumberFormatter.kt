package com.censustracker.util

/**
 * Phone number normalization helpers.
 * Stored format is always international-style: "+" followed by digits only.
 */
object PhoneNumberFormatter {

    /**
     * Normalize a raw user-entered number into "+<digits>" form.
     * - Already starts with "+" -> digits kept as-is (country code included).
     * - Otherwise the [countryCode] (e.g. "+91") is prepended, unless the
     *   entered digits already begin with that country's dial digits.
     */
    fun normalize(raw: String, countryCode: String = "+91"): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 7) return raw.trim()

        if (raw.trim().startsWith("+")) {
            return "+" + digits
        }

        val ccDigits = countryCode.filter { it.isDigit() }
        val national = if (ccDigits.isNotEmpty() && digits.startsWith(ccDigits) && digits.length > ccDigits.length + 6) {
            // User already typed the country code without the "+"
            digits
        } else {
            ccDigits + digits
        }
        return "+" + national
    }

    /** wa.me requires digits only, no "+" or separators. */
    fun toWhatsAppNumber(phoneNumber: String): String = phoneNumber.filter { it.isDigit() }
}
