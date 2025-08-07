package com.mtb.mobilebanking

import android.util.Base64
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.GeneralSecurityException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Created by Md Arifur Rahaman on 14/5/25.
 *Copyright © 2017 Mutual Trust Bank Limited
 */
class AndroidCypher {



    companion object {

        init {

        }
    }
    private val characterEncoding = "UTF-8"
    private val cipherTransformation = "AES/CBC/PKCS5Padding"
    private val aesEncryptionAlgorithm = "AES"
    var myKey =
        byteArrayOf(27, 23, 59, 84, 72, 46, 24, 51, 15, 27, 46, 20, 34, 55, 32, 115)
    var myIV =
        byteArrayOf(51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51)

    @Throws(
        UnsupportedEncodingException::class,
        InvalidKeyException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidAlgorithmParameterException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class
    )
    fun encrypt(var1: String): String {
        var var2 = var1.toByteArray(charset("UTF-8"))
        val var3 = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val var4 =
            SecretKeySpec(myKey, "AES")
        val var5 = IvParameterSpec(myIV)
        var3.init(1, var4, var5)
        var2 = var3.doFinal(var2)
        return Base64.encodeToString(var2, Base64.NO_WRAP)
    }

    @Throws(
        KeyException::class,
        GeneralSecurityException::class,
        GeneralSecurityException::class,
        InvalidAlgorithmParameterException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        IOException::class
    )
    fun decrypt(var1: String?): String {
        var var3 = Base64.decode(var1, Base64.DEFAULT)
        val var4 = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val var5 =
            SecretKeySpec(myKey, "AES")
        val var6 = IvParameterSpec(myIV)
        var4.init(2, var5, var6)
        var3 = var4.doFinal(var3)
        return String(var3,  Charsets.UTF_8)
    }
}