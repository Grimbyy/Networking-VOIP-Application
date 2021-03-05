package VOIP;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;
import Encryption.*;
import uk.ac.uea.cmp.voip.*;

import Configuration.ProgramSettings;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.*;

public class VOIP<T extends DatagramSocket, E extends Cryptography> {

    ProgramSettings settings;
    InetAddress dest;
    boolean CallActive = true;

    T SendingSocket;
    T ReceivingSocket;
    E EncryptionMethod;

    public VOIP(ProgramSettings importedSettings, String IPAddress) throws UnknownHostException {

        this.settings = importedSettings;
        this.dest = InetAddress.getByName(IPAddress);

        try {
            switch (settings.getDataSocket()) {
                case 0:
                    SendingSocket = (T) new DatagramSocket();
                    ReceivingSocket = (T) new DatagramSocket(settings.getReceivePort());
                    System.out.println("DS1");
                    break;
                case 1:
                    SendingSocket = (T) new DatagramSocket2();
                    ReceivingSocket = (T) new DatagramSocket2(settings.getReceivePort());
                    System.out.println("DS2");
                    break;
                case 2:
                    SendingSocket = (T) new DatagramSocket3();
                    ReceivingSocket = (T) new DatagramSocket3(settings.getReceivePort());
                    System.out.println("DS3");
                    break;
                case 3:
                    SendingSocket = (T) new DatagramSocket4();
                    ReceivingSocket = (T) new DatagramSocket4(settings.getReceivePort());
                    System.out.println("DS4");
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + settings.getDataSocket());
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        switch (settings.getEncryptionType()) {
            case 0:
                EncryptionMethod = (E) new Cryptography();
                break;
            case 1:
                //Async Key Generation
                EncryptionMethod = (E) new Asymmetric();
                break;
            case 2:
                //XOR
                EncryptionMethod = (E) new XOR(settings.getXORKey());
                break;
            case 3:
                //AES
                EncryptionMethod = (E) new AES();
                break;
            case 4:
                //Blowfish
                EncryptionMethod = (E) new Blowfish();
                break;
            //End of Encryption
        }

    }

    public void start() {
        Thread Sender = new Thread(new Send());
        Thread Receiver = new Thread(new Receive());
        Sender.start();
        Receiver.start();
        try {
            Sender.join();
            Receiver.join();
        } catch (InterruptedException e) {
            System.err.println("Threading Interrupted");
        }
    }

    class Send extends Thread {

        private byte[][] rotate(byte[][] A, int degrees) {
            if (degrees == 90) {
                final int AWidth = A.length;
                final int AHeight = A[0].length;

                byte[][] transposed = new byte[AHeight][AWidth];

                for (int w = 0; w < AWidth; w++) {
                    for (int h = 0; h < AHeight; h++) {
                        transposed[h][AWidth - 1 - w] = A[w][h];
                    }
                }

                return transposed;
            } else if (degrees == -90) {
                final int AWidth = A.length;
                final int AHeight = A[0].length;

                byte[][] transposed = new byte[AHeight][AWidth];

                for (int w = 0; w < AWidth; w++) {
                    for (int h = 0; h < AHeight; h++) {
                        transposed[h][AHeight - 1 - h] = A[w][h];
                    }
                }

                return transposed;
            }
            return A;
        }

        @Override
        public void run() {

            try {
                AudioRecorder rec = new AudioRecorder();
                while (CallActive) {

                    byte[] buffer = rec.getBlock();
                    if (settings.getInterleaverSize() > 1)
                    {
                        //Interleaver [Encrypt --> Rotate]
                        byte[][] queue = new byte[settings.getInterleaverSize()*settings.getInterleaverSize()][512];
                        queue[0] = buffer;
                        for (int i = 1; i<settings.getInterleaverSize()*settings.getInterleaverSize(); i++) {
                            queue[i] = EncryptionMethod.encrypt(rec.getBlock());
                        }

                        queue = rotate(queue, 90);

                        for (int i=0;i < queue.length;i++) { //Send packets
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, dest, settings.getReceivePort());
                            SendingSocket.send(packet);
                        }

                        //End of Interleaver
                        continue; //Restart while loop
                    }

                    if (settings.getAuthKeyEnabled()) { //Add auth key

                    }

                    //Encrypt
                    EncryptionMethod.encrypt(buffer);
                    //Send buffer (unreachable if using interleaver)
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, dest, settings.getReceivePort());
                    SendingSocket.send(packet);

                }
                SendingSocket.close();
            } catch (LineUnavailableException e) {
                System.err.println("Line Unavailable please check your audio settings then try again.");
                //e.printStackTrace();
                //CallActive = false;
            } catch (IOException e) {
                System.err.println("Random IO Exception Occurred.");
                CallActive = false;
            }

        } //End of Sending Thread

    }

    class Receive extends Thread {

        @Override
        public void run() {
            try {
                AudioPlayer player = new AudioPlayer();
                ReceivingSocket.setSoTimeout(5000);

                while (CallActive) {
                    byte[] buffer = new byte[512];
                    DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);

                    ReceivingSocket.receive(packet);

                    //Decryption

                    //End of Decryption
                    //Reverse Interleaver

                    //End of Interleaver

                    //Compensation somewhere here

                    player.playBlock(buffer);
                }
                ReceivingSocket.close();

            } catch (LineUnavailableException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                System.err.println("Failed to set socket exception");
            } catch (SocketTimeoutException e) {
                System.err.println("Lost connection to " + dest.getHostName());
                CallActive = false;
            } catch (IOException e) {
                System.err.println("Random IO Exception Occurred");
            }
        }

    }
}
