package hsm.demo.websocketwearablebridge;

import org.json.JSONObject;

public class MyMessageEvent {
    private MyMessage message;

    public MyMessageEvent(MyMessage msg){
        message=msg;
    }

    public JSONObject getMessage(){
        return message.getJsonObject();
    }
}
