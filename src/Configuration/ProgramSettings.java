package Configuration;

import java.util.ArrayList;

public class ProgramSettings {

    //0 = default, 1 = DatagramSocket2, 2 = DatagramSocket3, 3 = DatagramSocket4
    private int SelectedDataSocket;

    private int queueLength; //Queue of packets if interleaver is not being used

    // 0 = none, 1 = ASYNC KEY 2 = XOR, 3 = AES, 4 = Blowfish
    private int EncryptionType;
    private int XORKey = 343;

    //0 = none, 1 = Splicing, 2 = Fill-in, 3 = Repetition, 4 = Interpolation
    private int CompensationType;

    //4 = 4*4 etc; 0 = disabled.
    private int interleaverSize;

    //Enable or disable & set authkey
    private boolean AuthKeyEnabled;
    private int AuthKey;

    //Default Ports
    private int ReceivePort = 10011;

    private int timeout;

    public ProgramSettings(int DatagramSocket, int Compensation, int Encryption, boolean EnableAuthKey, int Key,  int interleaverSize, int queueLength) {
        this.SelectedDataSocket = DatagramSocket;
        this.EncryptionType = Encryption;
        this.CompensationType = Compensation;
        this.AuthKeyEnabled = EnableAuthKey;
        this.AuthKey = Key;
        this.interleaverSize = interleaverSize;
        this.queueLength = queueLength;
    }

    //Getters
    public void setDataSocket(int n) {
        if (n <= 3 && n >= 0) {
            this.SelectedDataSocket = n;
        } else {    System.err.println("Invalid input '"+n+"' for Datasocket Selection");   }
    }
    public void setEncryptionType(int n) {
        if (n <= 4 && n >= 0) {
            this.EncryptionType = n;
        } else {    System.err.println("Invalid input '"+n+"' for Encryption Selection");   }
    }
    public void setCompensationType(int n) {
        if (n <= 3 && n >= 0) {
            this.CompensationType = n;
        } else {    System.err.println("Invalid input '"+n+"' for Compensation Selection");   }
    }
    public void toggleAuthKey() {
        this.AuthKeyEnabled = !this.AuthKeyEnabled;
    }
    public void setAuthKey(int key) {
        this.AuthKey = key;
    }
    public void setInterleaverSize(int n) {
        this.interleaverSize = n;
    }
    public void setReceivePort(int n) { this.ReceivePort = n; }
    public void setQueueLength(int n) { this.queueLength = n; }
    public void setXORKey(int n) { this.XORKey = n; }

    //Setters
    public int getDataSocket() {
        return this.SelectedDataSocket;
    }
    public int getEncryptionType() {
        return this.EncryptionType;
    }
    public int getCompensationType() {
        return this.CompensationType;
    }
    public boolean getAuthKeyEnabled() {
        return this.AuthKeyEnabled;
    }
    public int getAuthKey() {
        return this.AuthKey;
    }
    public int getInterleaverSize() {
        return this.interleaverSize;
    }
    public int getReceivePort() { return this.ReceivePort; }
    public int getXORKey() { return this.XORKey; }
    public int getQueueLength() { return this.queueLength; }




    /*
    Types of compensation
    Splicing: Slapping the received parts together
    Fill-in: Add noise to packet loss
    Repetition: Just play the last thing you received
    Interpolation: Knowledge of Before and after packets of speech to fill in gap

    Encryption:
    As stated above EncryptionType Variable (ASYNC KEY, XOR, AES...)
    Useful article on VOIP encryption https://core.ac.uk/download/pdf/9628442.pdf

    Aiming for a fast encryption method to reduce effects on QoS
    According to the article AES and Blowfish Encryption are among the better methods for VOIP
     */
}
