package Compensation;

import java.nio.ByteBuffer;
import java.util.ArrayList;

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
                System.out.println(i + " - [" + OutBlock.getInt() +": ["+OutBlock.getInt()+"]]");
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

        //printBlock(B, 0);

        return B;
    }

    public byte[][] sortQueue(byte[][] A) {

        boolean[] There = new boolean[A.length];
        byte[][] B = new byte[A.length][A[0].length-4];

        boolean sorted = false;
        while (!sorted) {

            sorted = true;

            for (int i = 1; i < A.length; i++) {
                ByteBuffer OutBlock = ByteBuffer.wrap(A[i]);
                ByteBuffer OutBlock1 = ByteBuffer.wrap(A[i-1]);
                //System.out.println(A[i].length);
                int key = OutBlock.getInt();
                int key2 = OutBlock1.getInt();
                if (key < key2) {
                    sorted = false;
                    byte[] temp = A[i];
                    A[i] = A[i-1];
                    A[i-1] = temp;
                }
            }
        }

        //printBlock(A, 1);

        int targetNo = 0;
        int burstCount = 0;
        ArrayList<Integer> seenKeys = new ArrayList<>();
        for (int i=0;i<A.length;i++) {
            ByteBuffer OutBlock = ByteBuffer.wrap(A[i]);
            int key = OutBlock.getInt();
            if (!seenKeys.contains(key)) {
                B[targetNo++] = OutBlock.get(4, A[i], 0, A[i].length - 4).array();
                seenKeys.add(key);
                burstCount--;
            } else {
                burstCount++;
                if (burstCount >= 5) {
                    B[targetNo++] = OutBlock.get(4, A[i], 0, A[i].length - 4).array();
                    burstCount = 0;
                } else if ((i+burstCount) > 0 && (i+burstCount < A.length)) {
                    B[targetNo++] = OutBlock.get(4, A[i+burstCount], 0, A[i+burstCount].length - 4).array();
                }
            }
        }

        //printBlock(B, 1);

        return B;
    }
}
