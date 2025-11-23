package com.aryanspatel.moodmatch.domain.Crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val aead: Aead


    init {
        // Register all Tink primitives
        TinkConfig.register()

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(
                context,
                "moodmatch_keyset",  // filename for keyset
                "moodmatch_prefs"   // sharedPreferences name
            )
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://moodmatch_master_key")
            .build()
            .keysetHandle

        aead = keysetHandle.getPrimitive(Aead::class.java)
    }


    fun encrypt(plainText: String): String {
        val cipherBytes = aead.encrypt(
            plainText.toByteArray(Charsets.UTF_8),
            /* associatedData = */ null
        )
        return Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String): String {
        val cipherBytes = Base64.decode(cipherText, Base64.NO_WRAP)
        val plainBytes = aead.decrypt(cipherBytes, /* associatedData = */ null)
        return plainBytes.toString(Charsets.UTF_8)
    }
}
