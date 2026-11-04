package com.example.detoxproject.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val DEFAULT_ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_BYTES = 16

    /**
     * Generates a cryptographically secure 16-byte random salt as a hex string (32 chars).
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(SALT_BYTES)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes a password using PBKDF2-HMAC-SHA256 with 10,000 iterations and a 256-bit key length.
     * Stored format: "pbkdf2:10000:<derivedHashHex>"
     */
    fun hashPassword(password: String, salt: String, iterations: Int = DEFAULT_ITERATIONS): String {
        val saltBytes = hexToBytes(salt)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, iterations, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
        return "pbkdf2:$iterations:$hashHex"
    }

    /**
     * Checks whether the stored hash is from the legacy plain SHA-256 scheme.
     */
    fun isLegacyHash(storedHash: String): Boolean {
        return !storedHash.startsWith("pbkdf2:")
    }

    /**
     * Verifies a password against the stored hash and salt.
     * Supports both new PBKDF2-HMAC-SHA256 hashes and legacy SHA-256 hashes for transparent migration.
     * Uses constant-time byte comparison to protect against timing attacks.
     */
    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        return if (expectedHash.startsWith("pbkdf2:")) {
            val parts = expectedHash.split(":")
            if (parts.size != 3) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            val expectedHashHex = parts[2]

            val saltBytes = hexToBytes(salt)
            val spec = PBEKeySpec(password.toCharArray(), saltBytes, iterations, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val computedBytes = factory.generateSecret(spec).encoded
            val expectedBytes = hexToBytes(expectedHashHex)

            MessageDigest.isEqual(computedBytes, expectedBytes)
        } else {
            // Legacy plain SHA-256 check
            val legacyComputed = legacyHashPassword(password, salt)
            MessageDigest.isEqual(
                legacyComputed.toByteArray(Charsets.UTF_8),
                expectedHash.toByteArray(Charsets.UTF_8)
            )
        }
    }

    private fun legacyHashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$password"
        val digest = md.digest(combined.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val upper = Character.digit(hex[i], 16)
            val lower = Character.digit(hex[i + 1], 16)
            if (upper == -1 || lower == -1) break
            data[i / 2] = ((upper shl 4) + lower).toByte()
            i += 2
        }
        return data
    }
}
