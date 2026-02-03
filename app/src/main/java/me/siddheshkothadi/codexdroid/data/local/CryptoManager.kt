package me.siddheshkothadi.codexdroid.data.local

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "CryptoManager"
    private val aead: Aead

    init {
        AeadConfig.register()
        val keysetName = "codexdroid_keyset"
        val prefFileName = "codexdroid_crypto_prefs"
        val masterKeyUri = "android-keystore://codexdroid_master_key"

        fun buildWithMasterKey(): Aead {
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, keysetName, prefFileName)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(masterKeyUri)
                .build()
                .keysetHandle
            return keysetHandle.getPrimitive(Aead::class.java)
        }

        fun buildWithoutMasterKey(): Aead {
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, keysetName, prefFileName)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .build()
                .keysetHandle
            return keysetHandle.getPrimitive(Aead::class.java)
        }

        fun clearKeysetPrefs() {
            try {
                context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
            } catch (_: Exception) {
            }
        }

        fun deleteKeystoreKey(alias: String) {
            try {
                val ks = KeyStore.getInstance("AndroidKeyStore")
                ks.load(null)
                if (ks.containsAlias(alias)) {
                    ks.deleteEntry(alias)
                }
            } catch (_: Exception) {
            }
        }

        aead =
            try {
                buildWithMasterKey()
            } catch (e: Exception) {
                // Common on some devices/emulators or if keystore state is bad.
                Log.w(tag, "Keystore init failed, attempting recovery", e)
                clearKeysetPrefs()
                deleteKeystoreKey("codexdroid_master_key")

                try {
                    buildWithMasterKey()
                } catch (e2: Exception) {
                    // Last-resort: store the keyset unencrypted (still app-sandboxed), but avoid crashing at startup.
                    Log.w(tag, "Keystore recovery failed; falling back to unencrypted keyset", e2)
                    buildWithoutMasterKey()
                }
            }
    }

    fun encrypt(plaintext: String): String {
        val ciphertext = aead.encrypt(plaintext.toByteArray(), null)
        return Base64.encodeToString(ciphertext, Base64.DEFAULT)
    }

    fun decrypt(ciphertextBase64: String): String {
        val ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)
        val plaintext = aead.decrypt(ciphertext, null)
        return String(plaintext)
    }
}
