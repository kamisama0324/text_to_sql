package com.kami.springai.datasource.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

/**
 * 密码加密工具类，使用AES-256-CBC加密算法
 */
@Component
public class PasswordEncryptor {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY; // 从环境变量获取
    private static final String IV_STRING = "1234567890abcdef"; // 初始化向量
    
    static {
        // 添加BouncyCastle作为安全提供者
        Security.addProvider(new BouncyCastleProvider());
        
        // 从环境变量获取密钥，如果没有则使用默认值（生产环境必须设置环境变量）
        SECRET_KEY = System.getenv("DATASOURCE_SECRET_KEY") != null ? 
                     System.getenv("DATASOURCE_SECRET_KEY") : 
                     "default_secret_key_for_development_only_change_in_production";
    }
    
    /**
     * 加密密码
     */
    public String encrypt(String password) {
        try {
            byte[] secretKeyBytes = padKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            byte[] ivBytes = IV_STRING.getBytes(StandardCharsets.UTF_8);
            
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            
            byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
    
    /**
     * 解密密码
     */
    public String decrypt(String encryptedPassword) {
        try {
            byte[] secretKeyBytes = padKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            byte[] ivBytes = IV_STRING.getBytes(StandardCharsets.UTF_8);
            
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPassword);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("密码解密失败", e);
        }
    }
    
    /**
     * 填充密钥到32字节（AES-256）
     */
    private byte[] padKey(byte[] key) {
        byte[] paddedKey = new byte[32];
        System.arraycopy(key, 0, paddedKey, 0, Math.min(key.length, paddedKey.length));
        return paddedKey;
    }
}