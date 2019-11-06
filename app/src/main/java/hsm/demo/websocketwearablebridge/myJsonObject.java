package hsm.demo.websocketwearablebridge;

import org.json.JSONObject;

public class myJsonObject {

    JSONObject _jsonObject=null;

    public myJsonObject(int cMESSAGE, String sData){
        JSONObject jsonObject=new JSONObject();
        try {
            jsonObject.put("MESSAGE", cMESSAGE);
            jsonObject.put("DATA", sData);
        }catch(Exception ex){

        }
    }
    public JSONObject getJsonObj(){
        return _jsonObject;
    }

    public static String getMessage(String sJSON){
        String msg="UNKNOWN";
        try {
            JSONObject jsonObject = new JSONObject(sJSON);
            msg=jsonObject.getString("MESSAGE");
        }catch (Exception ex){

        }
        return msg;
    }
}
