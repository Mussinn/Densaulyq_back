package com.example.MedSafe.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Data
@AllArgsConstructor
public class RSAKeyPair {
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
}

