/*
 * Copyright 2026 The cylinder-backend Project under the WTFPL License,
 *
 *     http://www.wtfpl.net/about/
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * 代码千万行，注释第一行，编程不规范，日后泪两行
 *
 */
package me.koma;

import me.zhengjie.config.properties.RsaProperties;
import me.zhengjie.utils.RsaUtils;
import org.junit.jupiter.api.Test;

/**
 * TODO
 *
 * @author koma at cylinder-backend
 * @since 2026/4/7
 */
public class PwdTest {
    
    @Test
    public void encryptPwd() throws Exception {
        
        String pri = "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEA0vfvyTdGJkdbHkB8mp0f3FE0GYP3AYPaJF7jUd1M0XxFSE2ceK3k2kw20YvQ09NJKk+OMjWQl9WitG9pB6tSCQIDAQABAkA2SimBrWC2/wvauBuYqjCFwLvYiRYqZKThUS3MZlebXJiLB+Ue/gUifAAKIg1avttUZsHBHrop4qfJCwAI0+YRAiEA+W3NK/RaXtnRqmoUUkb59zsZUBLpvZgQPfj1MhyHDz0CIQDYhsAhPJ3mgS64NbUZmGWuuNKp5coY2GIj/zYDMJp6vQIgUueLFXv/eZ1ekgz2Oi67MNCk5jeTF2BurZqNLR3MSmUCIFT3Q6uHMtsB9Eha4u7hS31tj1UWE+D+ADzp59MGnoftAiBeHT7gDMuqeJHPL4b+kC+gzV4FGTfhR9q3tTbklZkD2A==";
        String pub = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANL378k3RiZHWx5AfJqdH9xRNBmD9wGD2iRe41HdTNF8RUhNnHit5NpMNtGL0NPTSSpPjjI1kJfVorRvaQerUgkCAwEAAQ==";
        
        String pwd1 = "123456";
        String pwd2 = "654321";
        
        String p1 = RsaUtils.encryptByPublicKey(pub,pwd1);
        String p2 = RsaUtils.encryptByPublicKey(pub,pwd2);
        
        System.out.println("加密后的密码1: " + p1);
        System.out.println("加密后的密码2: " + p2);
        
        System.out.println("解密后的密码1: " + RsaUtils.decryptByPrivateKey(pri, p1));
        System.out.println("解密后的密码2: " + RsaUtils.decryptByPrivateKey(pri, p2));
    }
    
}
