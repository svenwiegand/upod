/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.vending.licensing;

import android.content.Context;
import android.provider.Settings;
import com.google.android.vending.licensing.util.Base64;
import com.google.android.vending.licensing.util.Base64DecoderException;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * An Obfuscator that uses AES to encrypt data.
 */
public class AESObfuscator implements Obfuscator {
    private static final String UTF8 = "UTF-8";
    private static final String KEYGEN_ALGORITHM = "PBEWITHSHAAND256BITAES-CBC-BC";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final byte[] IV =
        { -11, 111, -113, -34, 44, 36, -92, 116, -106, 113, 96, -10, -39, 30, 115, -102 };
    private static final String header = "IlCLs1994";

    private Cipher mEncryptor;
    private Cipher mDecryptor;

    public AESObfuscator(Context context) {
        try {
            final byte[] salt = IV;
            final String applicationId = "mobi.upod.app";
            final String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEYGEN_ALGORITHM);
            KeySpec keySpec =   
                new PBEKeySpec((applicationId + deviceId).toCharArray(), salt, 1024, 256);
            SecretKey tmp = factory.generateSecret(keySpec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
            mEncryptor = Cipher.getInstance(CIPHER_ALGORITHM);
            mEncryptor.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(IV));
            mDecryptor = Cipher.getInstance(CIPHER_ALGORITHM);
            mDecryptor.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(IV));
        } catch (GeneralSecurityException e) {
            // This can't happen on a compatible Android device.
            throw new RuntimeException("Invalid environment", e);
        }
    }

    public String obfuscate(String original, String key) {
        if (original == null) {
            return null;
        }
        try {
            // Header is appended as an integrity check
            final byte[] srcBytes = (header + key + original).getBytes(UTF8);
            final ByteBuffer src = ByteBuffer.wrap(srcBytes);
            final byte[] output = obfuscateBytes(src, mEncryptor.getOutputSize(srcBytes.length), 0);
            return Base64.encode(output);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Invalid environment", e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Invalid environment", e);
        }
    }

    private byte[] obfuscateBytes(ByteBuffer src, int outLength, int repetition) throws BadPaddingException, IllegalBlockSizeException, ShortBufferException {
        try {
            final ByteBuffer output = ByteBuffer.allocate(outLength);
            mEncryptor.doFinal(src, output);
            return output.array();
        } catch (ShortBufferException ex) {
            if (repetition > 10) {
                throw ex;
            }
            return obfuscateBytes(src, outLength + mEncryptor.getBlockSize(), repetition + 1);
        } catch (IllegalBlockSizeException ex) {
            if (repetition > 10) {
                throw new RuntimeException("Failed to allocate output buffer. outLength=" + outLength + "; blockSize=" + mEncryptor.getBlockSize());
            }
            if ((outLength % mEncryptor.getBlockSize()) != 0)
                outLength = mEncryptor.getBlockSize() * ((outLength / mEncryptor.getBlockSize()) + 1);
            else
                outLength += mEncryptor.getBlockSize();
            return obfuscateBytes(src, outLength, repetition + 1);
        }
    }

    public String unobfuscate(String obfuscated, String key) throws ValidationException {
        if (obfuscated == null) {
            return null;
        }
        try {
            String result = new String(mDecryptor.doFinal(Base64.decode(obfuscated)), UTF8);
            // Check for presence of header. This serves as a final integrity check, for cases
            // where the block size is correct during decryption.
            int headerIndex = result.indexOf(header+key);
            if (headerIndex != 0) {
                throw new ValidationException("Header not found (invalid data or key)" + ":" +
                        obfuscated);
            }
            return result.substring(header.length()+key.length(), result.length());
        } catch (Base64DecoderException e) {
            throw new ValidationException(e.getMessage() + ":" + obfuscated);
        } catch (IllegalBlockSizeException e) {
            throw new ValidationException(e.getMessage() + ":" + obfuscated);
        } catch (BadPaddingException e) {
            throw new ValidationException(e.getMessage() + ":" + obfuscated);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Invalid environment", e);
        }
    }
}
