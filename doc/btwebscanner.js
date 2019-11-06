var connection;
var BTState=0;
const returncodes = {
    BTCONNECT_OK:5,
    CONNECTED:4,
    SCAN_DATA:3,
    MSG_RECEIVED:2,
    SUCCESS:1,
    OPEN_FAILED:-1,
    BTCONNECT_FAILED:-2,
    NOT_CONNECTED:-3,
};

class BTWebScanner{

    constructor(onOpenCallback, onRecvCallback){
        //start web service connection
        doLog("new BTWebScanner");
        connection = new WebSocket('ws://127.0.0.1:12345');

        // When the connection is open, send some data to the server
        connection.onopen = function () {
          doLog("onopen");
          onOpenCallback("websocket onopen", returncodes.CONNECTED);
          connection.send('Ping'); // Send the message 'Ping' to the server
        };
        
        // Log errors
        connection.onerror = function (error) {
          //doLog("websocket error "+ error);
          onOpenCallback("websocket error", returncodes.NOT_CONNECTED);
        };
        
        // Log messages from the server
        connection.onmessage = function (e) {
            if(e.data.startsWith("SCAN_DATA")){
                onRecvCallback(e.data.substring("SCAN_DATA".length), returncodes.SCAN_DATA);
            }else if(e.data.startsWith("BTSTATE=")){
                BTState=e.data.substring("BTSTATE=".length);
                if(BTState=="3")
                    onRecvCallback("State=" + BTState + e.data, returncodes.BTCONNECT_OK);
                else
                    onRecvCallback("State=" + BTState + e.data, returncodes.BTCONNECT_FAILED);
            }else{
                onRecvCallback(e.data);
            }
            doLog("server: " + e.data);
        };
        //invoke callback
        //onOpenCallback("websocket ready");
    }

    connect(btMac, onConnectCallback){
        if(connection){
            connection.send("BTCONNECT" + btMac);
            onConnectCallback("BTCONNECT done", returncodes.BTCONNECT_OK);
        }else{
            onConnectCallback("no connection", returncodes.NOT_CONNECTED);
        }
    }
    disconnect(onDisconnectCallback){
        if(connection){
            connection.send("BTDISCONNECT");
            onDisconnectCallback("BT disconnected");
        }else{
            onConnectCallback("no connection");
        }
    }
    send(msg, onSendCallback){
        if(connection){
            connection.send(msg);
            onSendCallback("send done: " + msg, returncodes.SUCCESS)
        }else{
            onConnectCallback("no connection");
            onSendCallback("send done", returncodes.NOT_CONNECTED)
        }
    }
    doLog(msg){
        console.log("BTWebScanner: " + msg);
    }
}
