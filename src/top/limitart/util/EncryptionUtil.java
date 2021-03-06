/*
 * Copyright (c) 2016-present The Limitart Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.limitart.util;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * 对称加密验证工具
 *
 * @author hank
 */
public final class EncryptionUtil {
    private static final String TRANSFORMATION = "AES/CBC/NoPadding";
    private static final String ALGORITHM = "AES";
    private final Key generateKey;
    private byte[] iv;

    /**
     * 获取加密实例
     *
     * @param password
     * @param ivStr
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    public synchronized static EncryptionUtil getEncodeInstance(String password, String ivStr) {
        return new EncryptionUtil(password, ivStr);
    }

    /**
     * 获取解密实例
     *
     * @param password
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    public synchronized static EncryptionUtil getDecodeInstance(String password) {
        return new EncryptionUtil(password);
    }

    /**
     * 加密端构造函数
     *
     * @param password
     * @param ivStr
     */
    private EncryptionUtil(String password, String ivStr) {
        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] resultKey = new byte[0X10];
        System.arraycopy(bytes, 0, resultKey, 0, Math.min(resultKey.length, bytes.length));
        generateKey = new SecretKeySpec(resultKey, ALGORITHM);
        if (ivStr != null) {
            // 初始化16位向量
            this.iv = new byte[0X10];
            byte[] ivRaw = ivStr.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(ivRaw, 0, this.iv, 0, Math.min(this.iv.length, ivRaw.length));
        }
    }

    /**
     * 解密端构造函数
     *
     * @param password
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     */
    private EncryptionUtil(String password) {
        this(password, null);
    }

    /**
     * 加密二进制
     *
     * @param bytes
     * @return
     * @throws Exception
     */
    public String encode(byte[] bytes) throws Exception {
        // 加密验证
        int zeroFlag = 0;
        byte[] jsonByteFillZero;
        int len = bytes.length;
        if ((len & 0XF) != 0) {
            len = ((len >> 4) + 1) << 4;
            zeroFlag = len - bytes.length;
            jsonByteFillZero = new byte[len];
            System.arraycopy(bytes, 0, jsonByteFillZero, 0, bytes.length);
        } else {
            jsonByteFillZero = bytes;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, generateKey, new IvParameterSpec(this.iv));
        byte[] doFinal = cipher.doFinal(jsonByteFillZero);
        byte[] content = new byte[doFinal.length + this.iv.length];
        System.arraycopy(this.iv, 0, content, 0, this.iv.length);
        System.arraycopy(doFinal, 0, content, this.iv.length, doFinal.length);
        byte[] base64Encode = CodecUtil.toBase64(content);
        String b64Str = new String(base64Encode, StandardCharsets.UTF_8);
        String result = b64Str.replace('+', '-').replace('/', '_').replace('=', '.');
        if (zeroFlag > 0) {
            result = "$" + String.format("%02d", zeroFlag) + result;
        }
        return result;
    }

    /**
     * 加密字符串
     *
     * @param source
     * @return
     * @throws Exception
     */
    public String encode(String source) throws Exception {
        return encode(source.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解密字符串
     *
     * @param source
     * @return
     * @throws Exception
     */
    public String decode(String source) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        String tokenSource = source;
        int zeroFlag = 0;
        if (tokenSource.startsWith("$")) {
            zeroFlag = Integer.parseInt(tokenSource.substring(1, 3));
            tokenSource = tokenSource.substring(3);
        }
        String token = tokenSource.replace('-', '+').replace('_', '/').replace('.', '=');
        byte[] base64Decode = CodecUtil.fromBase64(token.getBytes(StandardCharsets.UTF_8));
        byte[] iv = new byte[0X10];
        byte[] content = new byte[base64Decode.length - iv.length];
        System.arraycopy(base64Decode, 0, iv, 0, iv.length);
        System.arraycopy(base64Decode, iv.length, content, 0, content.length);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, generateKey, new IvParameterSpec(iv));
        byte[] doFinal = cipher.doFinal(content);
        // 去补零
        if (zeroFlag > 0) {
            byte[] afterZero = new byte[doFinal.length - zeroFlag];
            System.arraycopy(doFinal, 0, afterZero, 0, afterZero.length);
            return new String(afterZero, StandardCharsets.UTF_8);
        } else {
            return new String(doFinal, StandardCharsets.UTF_8);
        }
    }
}
