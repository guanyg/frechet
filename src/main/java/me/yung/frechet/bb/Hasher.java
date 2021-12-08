package me.yung.frechet.bb;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {
    public static byte[] h(byte[] b) {
        try {
            MessageDigest hasher = MessageDigest.getInstance("SHA-256");
            return hasher.digest(b);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}
