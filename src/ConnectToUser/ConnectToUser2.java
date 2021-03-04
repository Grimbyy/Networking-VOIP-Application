package ConnectToUser;

import uk.ac.uea.cmp.voip.DatagramSocket2;

import java.io.IOException;
import java.net.*;

public class ConnectToUser2 implements Runnable {

    public static boolean debug = true;

    private static InetAddress RecipientIP;
    public static int ReceivePort = 10011;
    public static boolean connected;
    public static String connection_failure_reason;
    static DatagramSocket2 sending_socket, receiving_socket;

    static class EstConnection implements Runnable {
        static DatagramSocket2 two_receiving_socket;
        public static int code_to_send = 1; // 1 ==  I haven't received your heartbeat yet
        // 2 == I have received your heartbeat
        public static boolean s_a_r_running = true;

        public EstConnection() {
            try { //Try set up receiving port
                this.two_receiving_socket = new DatagramSocket2(ConnectToUser2.ReceivePort-2); //Receive heartbeat on port 10009
            } catch (SocketException e) {
                ConnectToUser2.connected = false;
                ConnectToUser2.connection_failure_reason = "Unable to open heartbeat port " + (ConnectToUser2.ReceivePort-2);
            }
        }

        @Override
        public void run() {
            Thread shb = new Thread(new SendHeartBeat());
            Thread rhb = new Thread(new ReceiveHeartBeat());
            shb.start();
            rhb.start();
            try {
                shb.join();
                rhb.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        static class SendHeartBeat implements Runnable {

            @Override
            public void run() {
                if (debug) {System.out.println("Beginning sending of packets to " + RecipientIP.getHostName() + ":" + (ConnectToUser2.ReceivePort-2));}
                while (s_a_r_running) {
                    byte[] buffer = new byte[]{(byte) code_to_send};
                    DatagramPacket packet = new DatagramPacket(buffer, 1, RecipientIP, ConnectToUser2.ReceivePort-2); //Both users send from any available port to 10011
                    try {
                        if (debug) {System.out.println("Port [" + sending_socket.getLocalPort() + "] Sending data to " + RecipientIP.getHostName() + ":" + (ConnectToUser2.ReceivePort-2));}
                        sending_socket.send(packet);
                    } catch (IOException e) {
                        System.err.println("Random IOException occurred");
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        static class ReceiveHeartBeat extends Thread {

            boolean running = true;

            @Override
            public void run() {
                while (s_a_r_running) {
                    byte[] buffer = new byte[1];
                    DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);

                    try {
                        if (debug) {System.out.println("Waiting for other client response on port " + two_receiving_socket.getLocalPort());}
                        two_receiving_socket.receive(packet);
                        if (debug) {System.out.println("Recieved packet from" + packet.getAddress());}
                    } catch (IOException e) {
                        System.err.println("Random IOException occurred");
                    }

                    if (buffer[0] == 2 && code_to_send == 2) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        s_a_r_running = false; //Both machines confirmed connection so kill send and receive threads.
                    } else if (buffer[0] == 1 && code_to_send == 1) {
                        code_to_send = 2;
                        if (debug) {System.out.println("Other user hasn't confirmed but we now are");}
                    } else if (buffer[0] == 2 && code_to_send == 1) {
                        code_to_send = 2;
                        if (debug) {System.out.println("Other user confirmed and so are we");}
                    }
                }
                if (debug) {System.out.println("Connection confirmed.");}
            }

        }
    }

    public ConnectToUser2(String IP) {
        connected = true;
        try {
            RecipientIP = InetAddress.getByName(IP); //Grab IP from input
            receiving_socket = new DatagramSocket2(ReceivePort);
            sending_socket = new DatagramSocket2();
        } catch (UnknownHostException ex) {
            connected = false;
            connection_failure_reason = "Unable to find host " + IP;
        } catch (SocketException ex) {
            connected = false;
            connection_failure_reason = "Unable to open socket on port " + ReceivePort;
            System.out.println("Socket failure");
        }
    }

    @Override
    public void run() {
        if (connected) {
            if (debug) {System.out.println("Connecting to user at " + RecipientIP.getHostAddress());}
            Thread est_con = new Thread(new EstConnection());
            est_con.start();
            try {
                est_con.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
