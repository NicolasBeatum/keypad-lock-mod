package com.keypadlock.util

import java.security.MessageDigest

/**
 * Hashea listas de digitos (0-9) server-side con SHA-256. El texto plano de
 * la password NUNCA se persiste ni se loguea -- solo este hash hexadecimal
 * se guarda en el block entity.
 */
object PasswordHasher {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8

    fun isValidLength(digits: List<Int>): Boolean =
        digits.size in MIN_LENGTH..MAX_LENGTH && digits.all { it in 0..9 }

    fun hash(digits: List<Int>): String {
        val raw = digits.joinToString(separator = "") { it.toString() }
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        val hex = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            hex.append(String.format("%02x", b))
        }
        return hex.toString()
    }

    fun matches(digits: List<Int>, storedHash: String?): Boolean {
        if (storedHash == null) return false
        return hash(digits) == storedHash
    }
}
