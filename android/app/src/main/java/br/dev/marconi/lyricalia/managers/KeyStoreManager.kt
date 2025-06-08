package br.dev.marconi.lyricalia.managers

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyStoreManager {
    constructor(keyAlias: String) {
        generateKey(keyAlias)
    }

    private fun generateKey(keyAlias: String) {
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val parameters = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()

        keyGen.init(parameters)
        keyGen.generateKey()
    }

    fun encrypt(data: String, keyAlias: String): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }

        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray())

        val encryptedWithIv = iv + encryptedData
        return Base64.encodeToString(encryptedWithIv, Base64.DEFAULT)
    }

    fun decrypt(encryptedData: String, keyAlias: String): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }

        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        val encryptedWithIv = Base64.decode(encryptedData, Base64.DEFAULT)

        val iv = encryptedWithIv.sliceArray(0..11)
        val encrypted = encryptedWithIv.sliceArray(12 until encryptedWithIv.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedData = cipher.doFinal(encrypted)
        return String(decryptedData)
    }

    companion object {
        const val SPOTIFY_CLIENT_ID_KEY = "SpotifyClientId"
        const val SPOTIFY_CLIENT_SECRET_KEY = "SpotifyClientSecret"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}