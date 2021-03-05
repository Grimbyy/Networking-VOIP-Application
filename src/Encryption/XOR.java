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
        return OutBlock.array();
    }

    @Override
    public byte[] decrypt(byte[] toEncrypt) {
        return encrypt(toEncrypt);
    }

}
