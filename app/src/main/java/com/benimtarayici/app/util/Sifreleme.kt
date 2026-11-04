package com.benimtarayici.app.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Gizli verileri (ör. VPN anahtarları) Android Keystore ile şifreler.
// Anahtar telefondan dışarı çıkmaz, uygulama silinince kaybolur.
object Sifreleme {

    private const val ANAHTAR_ADI = "tarayici_vpn_anahtari"
    private const val SAGLAYICI = "AndroidKeyStore"
    private const val DONUSUM = "AES/GCM/NoPadding"

    private fun anahtarAl(): SecretKey {
        val depo = KeyStore.getInstance(SAGLAYICI)
        depo.load(null)
        val mevcut = depo.getKey(ANAHTAR_ADI, null)
        if (mevcut is SecretKey) return mevcut

        val uretici = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, SAGLAYICI)
        uretici.init(
            KeyGenParameterSpec.Builder(
                ANAHTAR_ADI,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return uretici.generateKey()
    }

    fun sifrele(metin: String): String {
        val sifre = Cipher.getInstance(DONUSUM)
        sifre.init(Cipher.ENCRYPT_MODE, anahtarAl())
        val iv = sifre.iv
        val veri = sifre.doFinal(metin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + veri, Base64.NO_WRAP)
    }

    fun coz(kayit: String): String {
        val tum = Base64.decode(kayit, Base64.NO_WRAP)
        val iv = tum.copyOfRange(0, 12)
        val veri = tum.copyOfRange(12, tum.size)
        val sifre = Cipher.getInstance(DONUSUM)
        sifre.init(Cipher.DECRYPT_MODE, anahtarAl(), GCMParameterSpec(128, iv))
        return String(sifre.doFinal(veri), Charsets.UTF_8)
    }
}
