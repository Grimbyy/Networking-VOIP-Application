package Compensation;

import java.nio.ByteBuffer;

public class PacketSorting {

    public PacketSorting() {

    }

    private void printBlock(byte[][] A, int scenario) {
        if (scenario == 0) {
            System.out.println("Before Sort");
            for (int i = 0;i<A.length;i++) {
                ByteBuffer OutBlock = ByteBuffer.wrap(A[i]);
                System.out.println(i + " - [" + OutBlock.getInt() +": ["+OutBlock.getInt()+"]]");
            }
        } else {    System.out.println("After Sort");
            for (int i = 0;i<A.length;i++) {
                ByteBuffer OutBlock = ByteBuffer.wrap(A[i]);
                System.out.println(i + " - [unknwn: ["+OutBlock.getInt()+"]]");
            }
        }

    }

    public byte[][] appendOrder(byte[][] A) {

        byte[][] B = new byte[A.length][A[0].length+4];

        for (int i = 0; i<A.length;i++) {


            ByteBuffer OutBlock = ByteBuffer.allocate(A[i].length + 4);
            OutBlock.putInt(i);
            OutBlock.put(A[i]);

            B[i] = OutBlock.array();
        }

        return B;
    }

    public byte[][] sortQueue(byte[][] A) {
        byte[][] B = new byte[A.length][A[0].length-4];

        //printBlock(A, 0);

        for (int i = 0;i<A.length;i++) {
            ByteBuffer OutBlock = ByteBuffer.wrap(A[i]);
            //System.out.println(A[i].length);
            int key = OutBlock.getInt();
            if (key >= 0 && key <=15) {
                B[key] = OutBlock.get(4, A[i], 0, A[0].length - 4).array();
            }
        }

        //printBlock(B, 1);

        return B;
    }
}
