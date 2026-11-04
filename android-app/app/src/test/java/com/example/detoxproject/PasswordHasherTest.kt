package com.example.detoxproject

import com.example.detoxproject.data.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PasswordHasherTest {

    @Test
    fun testHashPassword_producesPbkdf2Format() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword("SuperSecure123!", salt)

        assertTrue(hash.startsWith("pbkdf2:10000:"))
        assertFalse(PasswordHasher.isLegacyHash(hash))
    }

    @Test
    fun testVerifyPassword_validPasswordMatches() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword("MySecretPassphrase", salt)

        val isValid = PasswordHasher.verifyPassword("MySecretPassphrase", salt, hash)
        assertTrue(isValid)
    }

    @Test
    fun testVerifyPassword_invalidPasswordRejected() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword("CorrectPassword", salt)

        val isValid = PasswordHasher.verifyPassword("WrongPassword", salt, hash)
        assertFalse(isValid)
    }

    @Test
    fun testVerifyPassword_legacySha256HashSupported() {
        // Construct legacy SHA-256 hash format: sha256("$salt:$password")
        val salt = PasswordHasher.generateSalt()
        val password = "legacy_user_pass"
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$password"
        val digestBytes = md.digest(combined.toByteArray(Charsets.UTF_8))
        val legacyHash = digestBytes.joinToString("") { "%02x".format(it) }

        assertTrue(PasswordHasher.isLegacyHash(legacyHash))
        assertTrue(PasswordHasher.verifyPassword(password, salt, legacyHash))
        assertFalse(PasswordHasher.verifyPassword("wrong_pass", salt, legacyHash))
    }

    @Test
    fun testIsLegacyHash_distinguishesPbkdf2FromSha256() {
        val salt = PasswordHasher.generateSalt()
        val pbkdf2Hash = PasswordHasher.hashPassword("test", salt)
        val legacyHex = "a".repeat(64)

        assertFalse(PasswordHasher.isLegacyHash(pbkdf2Hash))
        assertTrue(PasswordHasher.isLegacyHash(legacyHex))
    }

    @Test
    fun testHashPassword_uniqueSaltsProduceDifferentHashes() {
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()
        val hash1 = PasswordHasher.hashPassword("same_password", salt1)
        val hash2 = PasswordHasher.hashPassword("same_password", salt2)

        assertNotEquals(hash1, hash2)
    }
}
