var connection;
const returncodes = {
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
        doLog("new connection");
        connection = new WebSocket('ws://199.64.70.126:12345');

        // When the connection is open, send some data to the server
        connection.onopen = function () {
          doLog("onopen");
          connection.send('Ping'); // Send the message 'Ping' to the server
        };
        
        // Log errors
        connection.onerror = function (error) {
          doLog("websocket error "+ error);
        };
        
        // Log messages from the server
        connection.onmessage = function (e) {
            if(e.data.startsWith("SCAN_DATA")){
                onRecvCallback(e.data.substring("SCAN_DATA".length), returncodes.SCAN_DATA);
            }else{
                onRecvCallback(e.data);
            }
            doLog("server: " + e.data);
        };
          //invoke callback
        onOpenCallback("websocket ready");
    }

    connect(btMac, onConnectCallback){
        if(connection){
            connection.send("BTCONNECT" + btMac);
            onConnectCallback("BTCONNECT done");
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
            onSendCallback("send done")
        }else{
            onConnectCallback("no connection");
        }
    }
    doLog(msg){
        console.log("BTWebScanner: " + msg);
    }
}
