package hsm.demo.websocketwearablebridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;

import de.greenrobot.event.EventBus;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BTScannerService {
    // Debugging
    private static final String TAG = "MyBluetoothService";
    private static final boolean D = true;
    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothChat";
    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private BluetoothAdapter mAdapter=null;
    private Handler mHandler=null;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private CommunicationThread mCommunicationThread;
    private int mState;
    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BTScannerService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = Constants.STATE_NONE;
        mHandler = handler;
        //enable event receiving
        EventBus.getDefault().register(this);
    }
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        postMessage(""+state);
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");
/*
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        // Cancel any thread currently running a connection
        if (mCommunicationThread != null) {mCommunicationThread.cancel(); mCommunicationThread = null;}
        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(Constants.STATE_LISTEN);

 */
        setState(Constants.STATE_CONNECTED);
    }
    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);
        // Cancel any thread attempting to make a connection
        if (mState == Constants.STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }
        // Cancel any thread currently running a connection
        if (mCommunicationThread != null) {mCommunicationThread.cancel(); mCommunicationThread = null;}
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(Constants.STATE_CONNECTING);
    }
    /**
     * Start the CommunicationThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        // Cancel any thread currently running a connection
        if (mCommunicationThread != null) {mCommunicationThread.cancel(); mCommunicationThread = null;}
        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        // Start the thread to manage the connection and perform transmissions
        mCommunicationThread = new CommunicationThread(socket);
        mCommunicationThread.start();
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        postMessage(new MyMessage(MyMessage.eType.infoType, MyMessage.eSource.srcBTService, "connected to "+device.getName(),null));
        setState(Constants.STATE_CONNECTED);
    }
    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mCommunicationThread != null) {mCommunicationThread.cancel(); mCommunicationThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(Constants.STATE_NONE);
    }
    /**
     * Write to the CommunicationThread in an unsynchronized manner
     * @param out The bytes to write
     * @see CommunicationThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        CommunicationThread r;
        // Synchronize a copy of the CommunicationThread
        synchronized (this) {
            if (mState != Constants.STATE_CONNECTED) return;
            r = mCommunicationThread;
        }
        // Perform the write unsynchronized
        Log.d(TAG, "calling r.write: " + new String(out));
        r.write(out);
    }
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(Constants.STATE_LISTEN);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(Constants.STATE_LISTEN);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }
        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;
            // Listen to the server socket if we're not connected
            while (mState != Constants.STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    synchronized (BTScannerService.this) {
                        switch (mState) {
                            case Constants.STATE_LISTEN:
                            case Constants.STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case Constants.STATE_NONE:
                            case Constants.STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }
        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
                Log.d(TAG, "SOCKET connected");

            } catch (IOException e) {
                Log.d(TAG, "SOCKET connect FAILED: " + e.getMessage());
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BTScannerService.this.start();
                Log.d(TAG, "Restart to Connected mode");
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BTScannerService.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);
            Log.d(TAG, "Start the connected thread");
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class CommunicationThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public CommunicationThread(BluetoothSocket socket) {
            Log.d(TAG, "create CommunicationThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.d(TAG, "in/out streams assigned");
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            Log.i(TAG, "BEGIN mCommunicationThread");
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    byte[] bData=new byte[bytes];
                    System.arraycopy(buffer,0, bData,0,bytes);
                    // Send the obtained bytes to the UI Activity
                    Bundle bundle=new Bundle();
                    bundle.putInt("MESSAGE_READ", Constants.MESSAGE_READ);
                    bundle.putByteArray("DATA", bData);
                    Message msg = mHandler.obtainMessage(Constants.MESSAGE_READ);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                    Log.d(TAG, "bt recv: " + btScanCtrl.toHex(new String(bData)));
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                String sSend=new String(buffer, "UTF-8");
                if(sSend.equals("BEEP"))
                {
                    buffer=btScanCtrl.setDoBeep();
                }
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
                Log.d(TAG, "connected Thread write(): " + btScanCtrl.toHex(new String(buffer, "UTF-8")));
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    //handles control messages from base websocket class
    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(MyMessage mMsg) {
        String message=mMsg.toString();
        Log.d(TAG, "onEvent MyMessageEvent: " + message);
        //sendMessage(message);
/*
        if(message.startsWith(Constants.BT_CONNECT_MAC)){
            String sMac=message.substring(Constants.BT_CONNECT_MAC.length());
            btScannerService=new BTScannerService(m_context, m_handler);
            // Get local Bluetooth adapter
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //find device
            device = mBluetoothAdapter.getRemoteDevice(sMac);
            //connect to BT device
            btScannerService.connect(device);
            Log.d(TAG, "scanner service state="+btScannerService.getState());
        }
*/
    }

    void postMessage(String sData){
        MyMessage mMsg=MyMessage.newInfoMessage(sData, MyMessage.eSource.srcBTService);
        EventBus.getDefault().post(mMsg);
    }
    void postMessage(MyMessage mMsg){
        EventBus.getDefault().post(mMsg);
    }
}
