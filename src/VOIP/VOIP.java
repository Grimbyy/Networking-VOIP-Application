package VOIP;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;
import uk.ac.uea.cmp.voip.*;

import Configuration.ProgramSettings;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.*;

public class VOIP<T extends DatagramSocket> {

    ProgramSettings settings;
    InetAddress dest;
    boolean CallActive = true;

    T SendingSocket;
    T ReceivingSocket;

    public VOIP(ProgramSettings importedSettings, String IPAddress) throws UnknownHostException {

        this.settings = importedSettings;
        this.dest = InetAddress.getByName(IPAddress);

        try {
            switch (settings.getDataSocket()) {
                case 0:
                    SendingSocket = (T) new DatagramSocket();
                    ReceivingSocket = (T) new DatagramSocket(settings.getReceivePort());
                    break;
                case 1:
                    SendingSocket = (T) new DatagramSocket2();
                    ReceivingSocket = (T) new DatagramSocket2(settings.getReceivePort());
                    break;
                case 2:
                    SendingSocket = (T) new DatagramSocket3();
                    ReceivingSocket = (T) new DatagramSocket3(settings.getReceivePort());
                    break;
                case 3:
                    SendingSocket = (T) new DatagramSocket4();
                    ReceivingSocket = (T) new DatagramSocket4(settings.getReceivePort());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + settings.getDataSocket());
            }
        } catch (SocketException e) {
            e.printStackTrace();
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

        @Override
        public void run() {

            try {
                AudioRecorder rec = new AudioRecorder();
                while (CallActive) {

                    byte[] buffer = rec.getBlock();
                    //Interleaver

                    //End of Interleaver
                    //Encryption Here

                    //End of Encryption
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, dest, settings.getReceivePort());
                    SendingSocket.send(packet);
                }
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

                    player.playBlock(buffer);
                }

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
