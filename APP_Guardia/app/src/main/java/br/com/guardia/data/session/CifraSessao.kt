package br.com.guardia.data.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object CifraSessao {
    @Synchronized
    fun cifrar(alias: String, dados: ByteArray, contexto: ByteArray? = null): ByteArray {
        val chave = chave(alias) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(
                alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
            generateKey()
        }
        val cifra = Cipher.getInstance("AES/GCM/NoPadding")
        cifra.init(Cipher.ENCRYPT_MODE, chave)
        check(cifra.iv.size == 12)
        if (contexto != null) cifra.updateAAD(contexto)
        return cifra.iv + cifra.doFinal(dados)
    }

    @Synchronized
    fun decifrar(alias: String, dados: ByteArray, contexto: ByteArray? = null): ByteArray {
        require(dados.size >= 28)
        val cifra = Cipher.getInstance("AES/GCM/NoPadding")
        cifra.init(Cipher.DECRYPT_MODE, checkNotNull(chave(alias)), GCMParameterSpec(128, dados, 0, 12))
        if (contexto != null) cifra.updateAAD(contexto)
        return cifra.doFinal(dados, 12, dados.size - 12)
    }

    @Synchronized
    fun destruir(alias: String) {
        val chaves = keystore()
        if (chaves.containsAlias(alias)) chaves.deleteEntry(alias)
        check(!chaves.containsAlias(alias))
    }

    private fun chave(alias: String): SecretKey? = keystore().getKey(alias, null) as? SecretKey

    private fun keystore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
}
