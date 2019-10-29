package hsm.demo.websocketwearablebridge;

import androidx.annotation.Nullable;

import org.json.JSONObject;

public class MyMessage {
    public enum eType{
        unknownType,
        stateType,
        infoType,
        errorType,
        dataType,
        cmdType
    }
    public enum eSource{
        srcUnknown,
        srcWebsocketServer,
        srcBTSocket,
        srcBTService,
        srcService,
        srcBTdevice,
    }
    static class eMessageType{
        final static String unknownType="unknownType";
        final static String infoType="infoType";
        final static String errorType="errorType";
        final static String dataType="dataType";
        final static String cmdType="cmdType";
    }
    static class eMessageSource{
        final static String srcUnknown="srcBTSocket";
        final static String srcWebsocketServer="srcWebsocketServer";
        final static String srcBTSocket="srcBTSocket";
        final static String srcService="srcService";
        final static String srcBTdevice="srcBTdevice";
    }

    eType messageType;
    eSource messageSource;
    String sData;
    Object oData;

    JSONObject jsonObject;
    public MyMessage(){
        messageType= eType.unknownType;
        messageSource=eSource.srcUnknown;
        sData="";
        oData=null;
        jsonObject=new JSONObject();
        try {
            jsonObject.put("messageType", messageType);
            jsonObject.put("messageSource", messageSource);
            jsonObject.put("data",sData);
            jsonObject.put("object", oData);
        }catch (Exception e){}
    }

    public MyMessage(eType eTyp, eSource eSrc, String s, @Nullable Object o){
        messageType=eTyp;
        messageSource=eSrc;
        sData=s;
        oData=o;
    }
    public JSONObject getJsonObject(){
        jsonObject=new JSONObject();
        try {
            jsonObject.put("messageType", messageType);
            jsonObject.put("messageSource", messageSource);
            jsonObject.put("data",sData);
            jsonObject.put("object", oData);
        }catch (Exception e){}
        return jsonObject;
    }
    public String getString(){
        StringBuilder sb=new StringBuilder();
        sb.append("Type: " + messageType);
        sb.append(", Source: " + messageSource);
        sb.append(", Data: " + sData);
        if(oData!=null)
            sb.append(", oData: " + oData.toString());
        return sb.toString();
    }
}
