import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.*;

import Configuration.ProgramSettings;
import ConnectToUser.*;
import Encryption.Cryptography;
import VOIP.VOIP;

public class Main {
    private static ProgramSettings settings;
    private static boolean running = true;
    private static Scanner in = new Scanner(System.in);
    private static int currentSubMenu;

    private static String[] Student_IDs = new String[]{"100274245", "100243142"};
    private static String title = "===== VOIP Program [" + Student_IDs[0] + " & " + Student_IDs[1] + "] =====";

    static <T> void print(T toPrint) { //Because System.out.println() it rather tedious
        System.out.println(toPrint);
    }

    static void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    static int getInput() {
        print("---------------------------------------");
        System.out.print("-" +": ");
        return in.nextInt();
    }

    static String getInput(String title) {
        print("---------------------------------------");
        System.out.print(title +": ");
        return in.next();
    }

    static void Connect() {

        Thread connection;
        String IPAddress = getInput("Enter IP Address");
        switch (settings.getDataSocket()) {
            case 0:
                connection = new Thread(new ConnectToUser(IPAddress));
                break;
            case 1:
                connection = new Thread(new ConnectToUser2(IPAddress));
                break;
            case 2:
                connection = new Thread(new ConnectToUser3(IPAddress));
                break;
            case 3:
                connection = new Thread(new ConnectToUser4(IPAddress));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + settings.getDataSocket());
        }
        clear();
        print("");
        print("===== Starting Connection =====");
        connection.start();
        try {
            connection.join();
        } catch (InterruptedException e) {
            print("Connection Thread Interrupted, returning to main menu");
            MainMenu();
        }
        //Continue program here as will not get past here unless
        VOIP<DatagramSocket, Cryptography> voip;
        try {
            voip = new VOIP<>(settings, IPAddress);
            voip.start();
        } catch (UnknownHostException e) {
            System.err.println("Failed to get IP Address");
            MainMenu();
            return;
        }

        MainMenu();
    }

    //Main menu = 0
    //Connect = 1
    //Settings menu = 2
    static void Navigate(int dir) {
        switch (dir) {
            case 1:
                Connect();
                break;
            case 2:
                SettingsMenu();
                break;
            case 3:
                PresetSelection();
                break;
            default:
                if (currentSubMenu == 0 && dir == 0) { System.exit(-1);};
                MainMenu();
                break;
        }
    }

    //Main menu = 0
    //Connect = 1
    //Settings menu = 2
        //Socket Selection = 1
        //Encryption = 2
        //Compensation = 3
        //Interleaver = 4
        //Auth Key = 5

    static void Navigate(int dir, int dir2) {
        switch (dir) {
            case 1:

                break;
            case 2:
                switch (dir2)
                {
                    case 1:
                        DatagramSelection();
                        break;
                    case 2:
                        EncryptionSelection();
                        break;
                    case 3:
                        CompensationSelection();
                        break;
                    case 4:
                        InterleaverSelection();
                        break;
                    case 5:
                        AuthKeySelection();
                        break;
                    case 6:
                        QueueSizeSelection();
                        break;
                }
                break;
            default:
                MainMenu();
                break;
        }
    }

    static void SettingsMenu() {
        currentSubMenu = 2;
        clear();
        print(title);
        print("1. Datagram Socket Selection");
        print("2. Encryption Configuration");
        print("3. Compensation Configuration");
        print("4. Interleaver Configuration");
        print("5. Auth Key Configuration");
        print("6. Packet Queue Configuration");
        print("0. Back");

        int input = getInput();
        if (input == 0) {   Navigate(0);   } else { Navigate(currentSubMenu, input); };
    }

    //Settings Submenus
    static void DatagramSelection() {
        clear();
        print(title);
        print("   Currently Selected [" + (settings.getDataSocket()+1) + "]");
        print("1. DatagramSocket [Default]");
        print("2. DatagramSocket2");
        print("3. DatagramSocket3");
        print("4. DatagramSocket4");
        print("0. Back");

        int input = getInput();
        if (input == 0) {   Navigate(currentSubMenu);   } else {   settings.setDataSocket(input-1); Navigate(currentSubMenu, 1); };
    }

    static void EncryptionSelection() {
        clear();
        print(title);
        print("   Currently Selected [" + (settings.getEncryptionType()+1) + "]");
        print("1. None [Default]");
        print("2. Asynchronous Key Generation");
        print("3. XOR");
        print("4. AES");
        print("5. Blowfish");
        print("0. Back");

        int input = getInput();
        if (input == 0) {   Navigate(currentSubMenu);   } else {   settings.setEncryptionType(input-1); Navigate(currentSubMenu, 2); };
    }

    static void CompensationSelection() {
        clear();
        print(title);
        print("   Currently Selected [" + (settings.getCompensationType()+1) + "]");
        print("1. None [Default]");
        print("2. Splicing");
        print("3. Fill-in");
        print("4. Repetition");
        print("5. Packet Sorting");
        print("0. Back");

        int input = getInput();
        if (input == 0) {   Navigate(currentSubMenu);   } else {   settings.setCompensationType(input-1); Navigate(currentSubMenu, 3); };
    }

    static void InterleaverSelection() {
        clear();
        print(title);
        print("   Currently Selected [" + settings.getInterleaverSize() + "*" + settings.getInterleaverSize() + "]");
        print("0. Back");

        int input = getInput();
        if (input == 0) {   Navigate(currentSubMenu);   } else {   settings.setInterleaverSize(input); Navigate(currentSubMenu, 4); };
    }

    static void AuthKeySelection() {
        clear();
        print(title);
        print("   Current Key [" + settings.getAuthKey() + " : Enabled = " + settings.getAuthKeyEnabled() + "]");
        print("1. Enable/Disable");
        print("2. New Key");
        print("0. Back");

        int input = getInput();
        if (input == 0) {   Navigate(currentSubMenu);   } else {
            if (input == 1) {   settings.toggleAuthKey();  } else if (input == 2) {  input = getInput();   settings.setAuthKey(input);   };
            Navigate(currentSubMenu, 5);
        };
    }

    static void QueueSizeSelection() {
        clear();
        print(title);
        print("   Currently Selected ["+settings.getQueueLength()+"]");
        print("0. Back");

        int input = getInput();
        if (input == 0) {   Navigate(currentSubMenu);   } else {   settings.setQueueLength(input); Navigate(currentSubMenu, 6); };
    }

    //Settings Submenus
    static void PresetSelection() {
        currentSubMenu = 3;
        clear();
        print(title);
        print("---Current Settings---");
        print("   Datasocket [ " + (settings.getDataSocket()) + " ]");
        print("   Queue Length [ "+settings.getQueueLength() + " ]");
        print("   Encryption Type [ " + settings.getEncryptionType() + " ]");
        print("   Compensation Type [ " + settings.getCompensationType() + " ]");
        print("   Interleaver Size [ " + settings.getInterleaverSize() + " ]");
        print("   AuthKey [ enabled: " + settings.getAuthKeyEnabled() + "/ key: "+ settings.getAuthKey()+" ]");
        print("----------------------");

        print("1. Demo 1");
        print("2. Demo 2");
        print("3. Demo 3");
        print("4. Demo 4");
        print("0. Back");

        int input = getInput();
        if (input == 0) {   Navigate(0);   } else {   loadSettings(input); Navigate(currentSubMenu); };
    }

    static void loadSettings(int setting) {
        switch (setting) {
            case 1:
                settings = new ProgramSettings(0, 4, 0, false, 0, 4, 0);
                break;
            case 2: //
                settings = new ProgramSettings(1, 4, 0, false, 0, 0, 20);
                break;
            case 3:
                settings = new ProgramSettings(2, 4, 0, false, 0, 0, 20);
                break;
            case 4:
                settings = new ProgramSettings(0, 4, 2, true, 154, 4, 0);
                break;
            default:
                settings = new ProgramSettings(0, 0, 0, false, 0, 0, 0);
                break;
        }
    }

    static void MainMenu() {
        currentSubMenu = 0;
        clear();
        print(title);
        print("1. Connect");
        print("2. Settings");
        print("0. Exit");

        Navigate(getInput());
    }

    public static void main(String[] args) {
        settings = new ProgramSettings(0, 0, 0, false, 0, 0, 0);
        MainMenu();
    }


}
