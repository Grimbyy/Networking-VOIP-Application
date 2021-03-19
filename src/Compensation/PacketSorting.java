package Compensation;

import Configuration.ProgramSettings;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PacketSorting {

    public int[] packet_queue_count = new int[]{0,0,0,0,0,0,0};
    public byte[][][] packet_queue;

    public PacketSorting(ProgramSettings settings) {
        if (settings.getInterleaverSize() > 0) {
            packet_queue = new byte[7][settings.getInterleaverSize()*settings.getInterleaverSize()][settings.getCalculatedPacketLength()];
        } else if (settings.getQueueLength() > 0) {
            packet_queue = new byte[7][settings.getQueueLength()][settings.getCalculatedPacketLength()];
        }
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

    public byte[][] appendOrder(byte[][] A, int sendCount) {
        int queueID = sendCount % 7;

        byte[][] B = new byte[A.length][A[0].length+8];

        for (int i = 0; i<A.length;i++) {


            ByteBuffer OutBlock = ByteBuffer.allocate(A[i].length + 8);
            OutBlock.putInt(queueID);
            OutBlock.putInt(i);
            OutBlock.put(A[i]);

            B[i] = OutBlock.array();
        }

        return B; //Sends ID of blockgroup + key for individual sorting order
    }

    public byte[][] sortQueue(byte[][] A, int expectedBlock) {
        int EqueueID = expectedBlock % 7;
        //Take everything from what we've been fed
        //Put any other block items in their respective slots
        //Sort expected block + return

        for (int i = 0; i < A.length; i++) {
            ByteBuffer OutBlock = ByteBuffer.wrap(A[i]);
            int queueID = OutBlock.getInt();

            try {
                packet_queue[queueID][packet_queue_count[queueID]++] = OutBlock.get(4, A[i], 0, A[i].length - 4).array();
            } catch (ArrayIndexOutOfBoundsException e) {
                //This error is believed to be a Java Error as application doesn't break AND no index of 4 is accessed here (accept for inside OutBlock.get where the length is 512-516)
                /*e.printStackTrace();

                System.err.println("=========================");
                System.err.println("packet_queue.length = "+packet_queue.length+" accessed with queueID = " + queueID);
                System.err.println("packet_queue_count.length = "+packet_queue_count.length+" accessed with queueID = " + queueID);
                System.err.println("A.length = " + A.length + " accessed by i = " + i);
                System.err.println("A[i].length = " + A[i].length + " accessed at index = " + 4);
                System.err.println("=========================");*/
            }
        }

        boolean sorted = false;
        while (!sorted) {

            sorted = true;

            for (int i = 1; i < packet_queue[EqueueID].length; i++) {
                ByteBuffer OutBlock = ByteBuffer.wrap(packet_queue[EqueueID][i]);
                ByteBuffer OutBlock1 = ByteBuffer.wrap(packet_queue[EqueueID][i-1]);

                int key = OutBlock.getInt();
                int key2 = OutBlock1.getInt();
                if (key < key2) {
                    sorted = false;
                    byte[] temp = packet_queue[EqueueID][i];
                    packet_queue[EqueueID][i] = packet_queue[EqueueID][i-1];
                    packet_queue[EqueueID][i-1] = temp;
                }
            }
        }

        //Grab our packets to send
        byte[][] requested_packets = packet_queue[EqueueID];
        byte[][] packets_to_play = new byte[requested_packets.length][requested_packets[0].length-4];

        for (int i = EqueueID; i>=0; i--) { //Reset past packets so we don't hear past speech repeated
            packet_queue[i] = new byte[packet_queue[i].length][packet_queue[i][0].length];
            packet_queue_count[i] = 0;
        }

        //Remove ordering INTS from packets inside our variable we grabbed (unless it sounds like a radar gun)
        for (int i = 0; i<requested_packets.length;i++) {
            ByteBuffer OutBlock = ByteBuffer.wrap(requested_packets[i]);
            int sort_key = OutBlock.getInt();
            packets_to_play[i] = OutBlock.get(4, requested_packets[i], 0, requested_packets[i].length-4).array();
        }

        return packets_to_play;

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
