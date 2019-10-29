package hsm.demo.websocketwearablebridge;

public class Constants {
    public static final String PREFS = "websocketwearablebridge_PREFS";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    //public static final String MESSAGE_READ_STR = "MESSAGE_READ";
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String MSG = "MSG";

    public static final int REQUEST_WRITE = 112;
    public static final int REQUEST_BTADMIN = 113;
    public static final int REQUEST_BT = 114;
    public static final int REQUEST_LOCATION = 115;
    public static final int REQUEST_RECEIVE_BOOT_COMPLETED=116;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public static final String BT_CONNECT_MAC = "BTCONNECT";
    public static final String BT_DISCONNECT ="BTDISCONNECT";

    public static final String START_ON_BOOT = "START_ON_BOOT";
}
