// FOR RESEARCH ONLY NOT PART OF SUBMISSION
// LEARNT FROM https://howtodoinjava.com/java/java-security/java-aes-encryption-example/
// !!NOT OUR WORK!!

package Encryption;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class AES extends Cryptography {

    boolean functional;

    private static SecretKeySpec myKey;
    private static byte[] bytekey;

    public AES(String key) {  // SEE DECLARATION AT TOP OF PAGE
        super();
        MessageDigest shakey = null;
        try {
            bytekey = key.getBytes("UTF-8");
            shakey = MessageDigest.getInstance("SHA-256");
            bytekey = shakey.digest(bytekey);
            bytekey = Arrays.copyOf(bytekey, 16);
            myKey = new SecretKeySpec(bytekey, "AES");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            System.err.println("Failed to establish AES key");
            functional = false;
        }
    }

    @Override
    public byte[] encrypt(byte[] toEncrypt) {
        try {

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, myKey);

            return Base64.getEncoder().encode(cipher.doFinal(toEncrypt));
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return toEncrypt;
    }

    @Override
    public byte[] decrypt(byte[] toDecrypt) {
        try {

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, myKey);

            return (cipher.doFinal(Base64.getDecoder().decode(toDecrypt)));
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return toDecrypt;
    }
}
