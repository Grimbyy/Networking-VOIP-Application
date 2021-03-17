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

                int packet_length = 512; //Standard Voice Block Length
                packet_length = settings.getAuthKeyEnabled() ? packet_length + 4 : packet_length; //Auth Key Length
                packet_length = settings.getCompensationType() == 4 ? packet_length + 4 : packet_length; //Packet Sorting Length

                System.out.println("Voice Chat Send Settings");
                System.out.println("Packet Length: " + packet_length);

                while (CallActive) {

                    //Interleave [Record --> Add Auth --> Encrypt --> Add Keys --> Rotate]
                    if (settings.getInterleaverSize() > 1) {
                        int size = settings.getInterleaverSize() * settings.getInterleaverSize();

                        //Queue of settings determined length
                        byte[][] queue = new byte[size][packet_length];
                        for (int i = 0; i < queue.length; i++) //Fill Queue
                        {
                            //Record
                            queue[i] = rec.getBlock();

                            //Add Auth
                            if (settings.getAuthKeyEnabled()) {
                                queue[i] = Auth.encrypt(queue[i]);
                            }

                            //Encrypt (will not encrypt if none selected)
                            queue[i] = EncryptionMethod.encrypt(queue[i]);
                        }

                        // Add keys (if enabled)
                        queue = settings.getCompensationType() == 4 ? pSorting.appendOrder(queue) : queue;

                        // Rotate
                        queue = interleaver.run(queue, "rotate");

                        //System.out.println("Sending Block ["+queue.length+"x"+queue[0].length+"]:");
                        for (int i = 0; i < queue.length; i++) {
                            //for (int j = 0;j < queue[i].length;j++) { System.out.print(queue[i][j] + ", "); }
                            //System.out.println();
                            DatagramPacket packet = new DatagramPacket(queue[i], queue[i].length, dest, settings.getReceivePort());
                            SendingSocket.send(packet);
                        }

                        continue;
                    }
                    //Queue (Interleaver takes precedence)
                    //[Record --> Add Auth --> Encrypt --> Add Keys]
                    if (settings.getQueueLength() > 1) {
                        int size = settings.getQueueLength();

                        //Queue of settings determined length
                        byte[][] queue = new byte[size][packet_length];
                        for (int i = 0; i < queue.length; i++) {

                            //Record
                            queue[i] = rec.getBlock();

                            //Add Auth
                            if (settings.getAuthKeyEnabled()) {
                                queue[i] = Auth.encrypt(queue[i]);
                            }

                            //Encrypt (will not encrypt if none selected)
                            queue[i] = EncryptionMethod.encrypt(queue[i]);

                        }

                        // Add keys (if enabled)
                        queue = settings.getCompensationType() == 4 ? pSorting.appendOrder(queue) : queue;

                        for (int i = 0; i < queue.length; i++) {
                            DatagramPacket packet = new DatagramPacket(queue[i], queue[i].length, dest, settings.getReceivePort());
                            SendingSocket.send(packet);
                        }

                        continue;
                    }

                    //No Queue no Interleaver (record --> add auth --> encrypt)
                    //Record
                    byte[] block = rec.getBlock();

                    //Add Auth
                    if (settings.getAuthKeyEnabled()) {
                        block = Auth.encrypt(block);
                    }

                    //Encrypt (will not encrypt if none selected)
                    block = EncryptionMethod.encrypt(block);

                    //Send
                    DatagramPacket packet = new DatagramPacket(block, block.length, dest, settings.getReceivePort());
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

                int packet_length = 512; //Standard Voice Block Length
                packet_length = settings.getAuthKeyEnabled() ? packet_length + 4 : packet_length; //Auth Key Length
                packet_length = settings.getCompensationType() == 4 ? packet_length + 4 : packet_length; //Packet Sorting Length

                ReceivingSocket.setSoTimeout(5000); //Timeout setup (5 Seconds)

                while (CallActive) {

                    //Interleave [Rotate --> Sort from Keys -->  Decrypt --> Check Auth --> Play]
                    if (settings.getInterleaverSize() > 1) {
                        //2D Array size
                        int size = settings.getInterleaverSize()*settings.getInterleaverSize();

                        byte[][] queue = new byte[packet_length][size];
                        for (int i = 0; i < queue.length;i++) {
                            DatagramPacket packet = new DatagramPacket(queue[i], 0, queue[i].length);
                            ReceivingSocket.receive(packet);
                        }

                        //Rotate Matrix (revert)
                        queue = interleaver.run(queue, "revert");

                        // Sort by Keys (if enabled)
                        queue = settings.getCompensationType() == 4 ? pSorting.sortQueue(queue) : queue;

                        for (int i=0;i< queue.length;i++) {
                            queue[i] = EncryptionMethod.decrypt(queue[i]);

                            if (settings.getAuthKeyEnabled() && Auth.checkAuthed(queue[i]))
                            {
                                queue[i] = Auth.decrypt(queue[i]);
                            } else if (settings.getAuthKeyEnabled() && !Auth.checkAuthed(queue[i]))
                            {
                                continue;
                            }

                            player.playBlock(queue[i]);
                        }

                        continue;
                    }
                    //Queue [Sort from Keys --> Decrypt --> Check Auth --> Play]
                    if (settings.getQueueLength() > 1) {
                        //2D Array size
                        int size = settings.getQueueLength();

                        byte[][] queue = new byte[size][packet_length];
                        for (int i = 0; i < queue.length;i++) {
                            DatagramPacket packet = new DatagramPacket(queue[i], 0, queue[i].length);
                            ReceivingSocket.receive(packet);
                        }

                        // Sort by Keys (if enabled)
                        queue = settings.getCompensationType() == 4 ? pSorting.sortQueue(queue) : queue;

                        for (int i=0;i< queue.length;i++) {
                            queue[i] = EncryptionMethod.decrypt(queue[i]);

                            if (settings.getAuthKeyEnabled() && Auth.checkAuthed(queue[i]))
                            {
                                queue[i] = Auth.decrypt(queue[i]);
                            }

                            player.playBlock(queue[i]);
                        }

                        continue;
                    }

                    //No Interleaving no Queue (Decrypt --> Check auth --> Play)
                    byte[] buffer = new byte[packet_length];

                    DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
                    ReceivingSocket.receive(packet);

                    buffer = EncryptionMethod.decrypt(buffer);

                    if (settings.getAuthKeyEnabled() && Auth.checkAuthed(buffer)) {
                        buffer = Auth.decrypt(buffer);
                        player.playBlock(buffer);
                    } else if (!settings.getAuthKeyEnabled()) {
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
