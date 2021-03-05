package Compensation;

import Configuration.ProgramSettings;

public class Interleaver {


    public Interleaver(ProgramSettings settings) {

    }

    public byte[][] run(byte[][] A, String mode) {
        return rotate(A, mode);
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

            return transposed;
        } else if (mode.equals("revert")) {
            final int AWidth = A.length; //512 ish
            final int AHeight = A[0].length; //interleaver size ie 4*4

            byte[][] transposed = new byte[AHeight][AWidth];

            for (int w = 0; w < AWidth; w++) {
                for (int h = 0; h < AHeight; h++) {
                    transposed[AHeight - 1 - h][w] = A[w][h];
                }
            }

            return transposed;
        }
        return A;
    }
}
