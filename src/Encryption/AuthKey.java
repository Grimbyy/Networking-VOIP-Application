package Encryption;

import java.nio.ByteBuffer;

public class AuthKey extends Cryptography {

    private int AuthKey;
    public AuthKey(int key) {
        super();
        this.AuthKey = key;
    }

    @Override
    public byte[] encrypt(byte[] toEncrypt) {
        ByteBuffer OutBlock = ByteBuffer.allocate(toEncrypt.length+4);
        OutBlock.putInt(this.AuthKey);
        OutBlock.put(toEncrypt);
        return OutBlock.array();
    }

    public boolean checkAuthed(byte[] block) {
        ByteBuffer buffer = ByteBuffer.wrap(block);
        if (buffer.getInt() == this.AuthKey) {
            return true;
        } else {
            return false;
        }
    }
}
