package VOIP;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;
import Compensation.Interleaver;
import Compensation.PacketSorting;
import Encryption.*;
import uk.ac.uea.cmp.voip.*;

import Configuration.ProgramSettings;

import javax.crypto.EncryptedPrivateKeyInfo;
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
    AuthKey Auth;
    Interleaver interleaver;
    PacketSorting pSorting;

    public VOIP(ProgramSettings importedSettings, String IPAddress) throws UnknownHostException {

        this.settings = importedSettings;
        this.dest = InetAddress.getByName(IPAddress);
        Auth = new AuthKey(settings.getAuthKey());
        interleaver = new Interleaver(settings);
        pSorting = new PacketSorting();

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

        @Override
        public void run() {

            try {
                AudioRecorder rec = new AudioRecorder();
                while (CallActive) {

                    byte[] buffer = rec.getBlock();
                    if (settings.getInterleaverSize() > 1)
                    {
                        int packet_size = 512;
                        if (settings.getCompensationType() == 4) {
                            packet_size = packet_size+4;
                        }
                        if (settings.getAuthKeyEnabled()) {
                            packet_size = packet_size+4;
                        }
                        //Interleaver [Encrypt --> Rotate --> add Order]
                        byte[][] queue;
                        if (settings.getAuthKeyEnabled()) { //Add auth key
                            queue = new byte[settings.getInterleaverSize()*settings.getInterleaverSize()][packet_size];
                            queue[0] = EncryptionMethod.encrypt(Auth.encrypt(buffer));
                        } else {
                            queue = new byte[settings.getInterleaverSize()*settings.getInterleaverSize()][packet_size];
                            queue[0] = EncryptionMethod.encrypt(buffer);
                        }

                        for (int i = 1; i<settings.getInterleaverSize()*settings.getInterleaverSize(); i++) {
                            if (settings.getAuthKeyEnabled()) { //Add auth key
                                queue[i] = Auth.encrypt(rec.getBlock());
                            }
                            queue[i] = EncryptionMethod.encrypt(queue[i]);
                        }

                        if (settings.getCompensationType() == 4) {
                            queue = pSorting.appendOrder(queue);
                        }

                        queue = interleaver.run(queue, "rotate");
                        //System.out.println("rotated length now = " + queue.length);
                        //System.out.println("rotated height now = " + queue[0].length);

                        for (int i=0;i < queue.length;i++) { //Send packets
                            DatagramPacket packet = new DatagramPacket(queue[i], queue[i].length, dest, settings.getReceivePort());
                            SendingSocket.send(packet);
                        }

                        //End of Interleaver
                        continue; //Restart while loop
                    }

                    if (settings.getQueueLength() > 0) {
                        int packet_size = 512;
                        if (settings.getCompensationType() == 4) {
                            packet_size = packet_size+4;
                        }
                        if (settings.getAuthKeyEnabled()) {
                            packet_size = packet_size+4;
                        }
                        byte[][] queue;
                        if (settings.getAuthKeyEnabled()) { //Add auth key
                            queue = new byte[settings.getQueueLength()][packet_size];
                            queue[0] = Auth.encrypt(buffer);
                        } else {
                            queue = new byte[settings.getQueueLength()][packet_size];
                            queue[0] = buffer;
                        }

                        for (int i = 1; i<queue.length; i++) {
                            if (settings.getAuthKeyEnabled()) { //Add auth key
                                queue[i] = Auth.encrypt(rec.getBlock());
                            }
                            queue[i] = EncryptionMethod.encrypt(queue[i]);
                        }

                        /*if (settings.getCompensationType() == 4) {
                            queue = pSorting.appendOrder(queue);
                        }*/

                        for (int i=0;i < queue.length;i++) { //Send packets
                            DatagramPacket packet = new DatagramPacket(queue[i], queue[i].length, dest, settings.getReceivePort());
                            SendingSocket.send(packet);
                        }

                        continue;
                    }

                    if (settings.getAuthKeyEnabled()) { //Add auth key
                        buffer = Auth.encrypt(buffer);
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
            } catch (Exception e) {
            System.err.println("Unknown Exception occurred");
            e.printStackTrace();
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
                int packet_length = 512;
                if (settings.getAuthKeyEnabled()) {
                    packet_length = packet_length + 4;
                }
                if (settings.getCompensationType() == 4) {
                    packet_length = packet_length + 4;
                }

                while (CallActive) {
                    byte[] buffer = new byte[packet_length];

                    int InterSize = settings.getInterleaverSize();
                    if (InterSize > 1) {
                        //Reverse Interleaver
                        byte[][] queue = new byte[packet_length][InterSize*InterSize];
                        for (int i = 0; i < queue.length; i++) {
                            DatagramPacket packet = new DatagramPacket(queue[i], 0, queue[i].length);
                            ReceivingSocket.receive(packet);
                        }

                        queue = interleaver.run(queue, "revert");
                        //System.out.println("rotated length now = " + queue.length);
                        //System.out.println("rotated height now = " + queue[0].length);

                        if (settings.getCompensationType() == 4) {
                            System.out.println("HERE");
                            queue = pSorting.sortQueue(queue);
                        }

                        //End of Interleaver
                        for (int i = 0; i < queue.length; i++) { //Decryption and play
                            queue[i] = EncryptionMethod.decrypt(queue[i]);
                            if (settings.getAuthKeyEnabled() && Auth.checkAuthed(queue[i]))
                            {
                                System.out.println("No queue, inter, auth");
                                player.playBlock(Auth.decrypt(queue[i]));
                            } else if (!settings.getAuthKeyEnabled()) {
                                System.out.println("No queue, inter, no auth");
                                player.playBlock(queue[i]);
                            }
                        }

                        //Return to top of loop
                        continue;
                    }

                    int QueueSize = settings.getQueueLength();
                    if (QueueSize > 0) {
                        byte[][] queue = new byte[settings.getQueueLength()][packet_length];
                        for (int i = 0; i < queue.length; i++)
                        {
                            DatagramPacket packet = new DatagramPacket(queue[i], 0, queue[i].length);
                            ReceivingSocket.receive(packet);
                        }

                        /*if (settings.getCompensationType() == 4) {
                            System.out.println("HERE2");
                            queue = pSorting.sortQueue(queue);
                        }*/

                        for (int i = 0; i < queue.length; i++) { //Decryption and play
                            queue[i] = EncryptionMethod.decrypt(queue[i]);
                            if (settings.getAuthKeyEnabled() && Auth.checkAuthed(queue[i]))
                            {
                                System.out.println("queue, no inter, auth");
                                player.playBlock(Auth.decrypt(queue[i]));
                            } else {
                                System.out.println("queue, no inter, no auth");
                                player.playBlock(queue[i]);
                            }
                        }

                        continue;
                    }


                    DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
                    ReceivingSocket.receive(packet);

                    buffer = EncryptionMethod.decrypt(buffer);

                    if (settings.getAuthKeyEnabled() && Auth.checkAuthed(buffer))
                    {
                        System.out.println("No queue, no inter, auth");
                        player.playBlock(Auth.decrypt(buffer));
                    } else if (!settings.getAuthKeyEnabled()) {
                        System.out.println("No queue, no inter, no auth");
                        player.playBlock(buffer);
                    }

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
            } catch (Exception e) {
                System.err.println("Unknown Exception occurred");
                e.printStackTrace();
                CallActive = false;
            }
        }

    }
}
