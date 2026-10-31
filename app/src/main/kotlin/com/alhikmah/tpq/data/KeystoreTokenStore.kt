package com.alhikmah.tpq.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.alhikmah.tpq.domain.api.TokenStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Menyimpan token akses terenkripsi: kunci AES-256 di Android Keystore (tidak bisa diekstrak dari perangkat),
 * data AES-GCM disimpan Base64 di SharedPreferences privat. `allowBackup` mati, jadi berkas ini tidak ikut cadangan.
 * Dipilih menggantikan EncryptedSharedPreferences (androidx.security-crypto sudah deprecated) tanpa dependensi tambahan.
 * Bila dekripsi gagal (kunci hilang/rusak), token dianggap tidak ada dan pengguna diminta masuk lagi.
 */
class KeystoreTokenStore(context: Context) : TokenStore {
    private val prefs = context.applicationContext.getSharedPreferences("auth", Context.MODE_PRIVATE)

    @Volatile
    private var cached: String? = null

    @Volatile
    private var loaded = false

    @Synchronized
    override fun get(): String? {
        if (!loaded) {
            cached = readStored()
            loaded = true
        }
        return cached
    }

    @Synchronized
    override fun save(token: String) {
        val stored = try {
            encrypt(token)
        } catch (_: Exception) {
            // Keystore tidak tersedia: lebih baik tidak menyimpan (login ulang tiap buka) daripada menyimpan polos.
            prefs.edit().clear().commit()
            cached = token
            loaded = true
            return
        }
        prefs.edit().putString(KEY_TOKEN, stored).commit()
        cached = token
        loaded = true
    }

    @Synchronized
    override fun clear() {
        prefs.edit().clear().commit()
        cached = null
        loaded = true
    }

    private fun readStored(): String? {
        val stored = prefs.getString(KEY_TOKEN, null) ?: return null
        return try {
            decrypt(stored)
        } catch (_: Exception) {
            prefs.edit().clear().commit()
            null
        }
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "alhikmah_token_key"
        const val KEY_TOKEN = "token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
