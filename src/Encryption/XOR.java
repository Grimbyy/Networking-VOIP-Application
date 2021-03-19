package Encryption;

import java.nio.ByteBuffer;

public class XOR extends Cryptography {

    private int XORKey;
    public XOR(int key) {
        super();
        this.XORKey = key;
    }

    @Override
    public byte[] encrypt(byte[] block) {
        ByteBuffer OutBlock = ByteBuffer.allocate(block.length);
        ByteBuffer InBlock = ByteBuffer.wrap(block);
        for (int j = 0; j < block.length/4; j++) {
            int byteBlock = InBlock.getInt();
            byteBlock = byteBlock ^ this.XORKey;
            OutBlock.putInt(byteBlock);
        }
        return encrypt(OutBlock.array(), this.XORKey*64);
    }


    public byte[] encrypt(byte[] block, int newkey) {

        ByteBuffer OutBlock = ByteBuffer.allocate(block.length);
        ByteBuffer InBlock = ByteBuffer.wrap(block);
        for (int j = 0; j < block.length / 4; j++) {
            int byteBlock = InBlock.getInt();
            byteBlock = byteBlock ^ newkey;
            OutBlock.putInt(byteBlock);
        }

        if (newkey > 0) {
            return encrypt(OutBlock.array(), (int) newkey/2);
        } else {
            return OutBlock.array();
        }
    }

    @Override
    public byte[] decrypt(byte[] toEncrypt) {
        return encrypt(toEncrypt);
    }

}
