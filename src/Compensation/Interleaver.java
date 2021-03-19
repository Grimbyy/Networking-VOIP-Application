package Compensation;

import Configuration.ProgramSettings;

import java.nio.ByteBuffer;

public class Interleaver {


    public Interleaver(ProgramSettings settings) {

    }

    public byte[][] run(byte[][] A, String mode) {
        //return rotate(A, mode);
        return rotate2(A, mode);
    }

    private byte[][] rotate(byte[][] A, String mode) {
        if (mode.equals("rotate")) {
            final int AWidth = A.length;
            final int AHeight = A[0].length;

            byte[][] transposed = new byte[AHeight][AWidth];

            for (int w = 0; w < AWidth; w++) {
                for (int h = 0; h < AHeight; h++) {
                    transposed[h][AWidth - 1 - w] = A[w][h];
                }
            }
            //System.out.println("Rotating: " + A[0][0] + ", " + A[0][1] + ", " + A[0][2] + ", " + A[0][3] + ", " + A[0][4]);
            //System.out.println("Rotated Array " + transposed.length + " in length and " + transposed[0].length + " in height");
            return transposed;
        } else if (mode.equals("revert")) {
            /*final int AWidth = A.length; //512 ish
            //System.out.println(AWidth);
            final int AHeight = A[0].length; //interleaver size ie 4*4
            //System.out.println(AHeight);
            byte[][] transposed = new byte[AHeight][AWidth];

            for (int w = 0; w < AWidth; w++) {
                for (int h = 0; h < AHeight; h++) {
                    transposed[AHeight - 1 - h][w] = A[w][h];
                }
            }
            //System.out.println("Reverted to: " + transposed[0][0] + ", " + transposed[0][1] + ", " + transposed[0][2] + ", " + transposed[0][3] + ", " + transposed[0][4]);
            //System.out.println("Reverted Array " + transposed.length + " in length and " + transposed[0].length + " in height");
            return transposed;*/

            return rotate(rotate(rotate(A, "rotate"), "rotate"), "rotate");
        }
        return A;
    }

    private byte[][] rotate2(byte[][] A, String mode) {
        byte[][] transposed = new byte[A.length][A[0].length];
        int interleaverXY = (int) Math.sqrt(A.length);
        //System.out.println(interleaverXY);
        if (mode.equals("rotate")) {

            for (int x = 0; x < interleaverXY; x++) {
                for (int y = 0; y < interleaverXY; y++) {
                    transposed[x * interleaverXY + y] = A[(interleaverXY - y - 1) * interleaverXY + x];
                    //System.out.println("rotate");
                }
            }

        } else {

            for (int x = 0; x < interleaverXY; x++) {
                for (int y = 0; y < interleaverXY; y++) {
                    transposed[(interleaverXY - y - 1) * interleaverXY + x] = A[x * interleaverXY + y];
                }
            }

        }
        return transposed;
    }
}
