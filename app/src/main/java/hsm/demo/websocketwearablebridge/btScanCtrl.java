package hsm.demo.websocketwearablebridge;

import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.lang.System.arraycopy;

public class btScanCtrl {
    final static String TAG="MyBTScanCtrl";

    public static String toHex(String sIN) {
        if ((sIN == null)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        char[] cArr = sIN.toCharArray();
        for (char c : cArr) {
            if ((c < ' ')) {
                sb.append(("<" + Integer.toHexString(((int) c)) + ">"));
            } else {
                sb.append(c);
            }

        }

        return sb.toString();
    }

    public static int toInt32_2(byte[] bytes, int index) {
        int a = (int) ((int) (0xff & bytes[index]) << 32 | (int) (0xff & bytes[index + 1]) << 40 | (int) (0xff & bytes[index + 2]) << 48 | (int) (0xff & bytes[index + 3]) << 56);
        // int a = (int)((int)(0xff & bytes[index]) << 56 | (int)(0xff & bytes[index + 1]) << 48 | (int)(0xff & bytes[index + 2]) << 40 | (int)(0xff & bytes[index + 3]) << 32);
        //Array.Resize;
        return a;
    }

    public static BarcodeData getBarcodeData(byte[] bData) {
        BarcodeData bcData = new BarcodeData(("   "));
        if (((bData[0] == 22)
                && ((bData[1] == 254)
                && (bData[6] == 13)))) {
            byte[] bLen = new byte[4];
            //arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
            //public static void ConstrainedCopy (Array sourceArray, int sourceIndex, Array destinationArray, int destinationIndex, int length);
            arraycopy(bData, 2, bLen, 0, 4); //Array.ConstrainedCopy(bData, 2, bLen, 0, 4);
            int uL = toInt32_2(bLen, 0);
            byte[] bDataMSG = new byte[uL];
            arraycopy(bData, 7, bDataMSG, 0, uL); //Array.ConstrainedCopy(bData, 7, bDataMSG, 0, ((int)(uL)));
            // => MSGGET0022IC1<1d>1012345678<1d>2187654321<d><0><0>
            String sMSG = new String(bDataMSG, StandardCharsets.UTF_8);
            // now we get the message length
            int iMsgLen = Integer.getInteger(sMSG.substring(6, 6+4));
            //  look at MSGGET0022...
            bcData = new BarcodeData(sMSG.substring(10, 10+3));
            bcData.sData = sMSG.substring(13, 13 + iMsgLen);
        }

        return bcData;
    }

    public static JSONObject getBarcodeDataJSON(byte[] bData) {
        JSONObject bcData = new JSONObject();
        if (((bData[0] == 22)
                && ((bData[1] == -2)
                && (bData[6] == 13)))) {
            byte[] bLen = new byte[4];
            arraycopy(bData, 2, bLen, 0, 4); //Array.ConstrainedCopy(bData, 2, bLen, 0, 4);
            int uL = toInt32_2(bLen, 0);
            byte[] bDataMSG = new byte[uL]; //12 is length of time stamp plus a 0x0d
            arraycopy(bData, 7, bDataMSG, 0, uL); //Array.ConstrainedCopy(bData, 7, bDataMSG, 0, ((int)(uL)));
            // => MSGGET0022IC1<1d>1012345678<1d>2187654321<d><0><0>
            //    0....5....1..5   ....2....5....3....5....4....5
            //              0         0         0         0

            String sMSG = new String(bDataMSG, StandardCharsets.UTF_8);
            // now we get the message length, example 0022
            int iMsgLen=0;
            try {
                Log.d(TAG, "Length= " +sMSG.length());
                String sCount=sMSG.length()>10 ? sMSG.substring(6,10) : sMSG.substring((6));
                iMsgLen = Integer.parseInt (sCount, 10);
            }catch(StringIndexOutOfBoundsException ex) {Log.d(TAG, "StringIndexOutOfBoundsException for parseInt for " + sMSG.substring(6, 4) + ": " + ex.getMessage());
            }catch(NumberFormatException ex){Log.d(TAG, "NumberFormatException for parseInt for "+sMSG.substring(6, 4)+": " + ex.getMessage());
            }catch(Exception ex){Log.d(TAG, "Exception for parseInt for "+sMSG.substring(6, 4)+": " + ex.getMessage());}
            String sHONid=sMSG.substring(10,11);
            String sAIMid=sMSG.substring(11,13);
            String sData=sMSG.substring(14,14+iMsgLen);
            //  look at MSGGET0022...
            try {
                bcData.put("HONID", sHONid);
                bcData.put("AIMID", sAIMid);
                bcData.put("DATA", sData);
//                bcData.put("TIME", sMSG.substring(iMsgLen+2, 12+iMsgLen));
            }catch (Exception ex){
                Log.d(TAG, "Exception: " + ex.getMessage());
            }
        }

        return bcData;
    }

    public static String sIsACK(String sIN) {
        int iPosAck = sIN.indexOf("\u0006");
        if ((iPosAck >= 0)) {
            return sIN.substring(0, iPosAck);
        }

        return "";
    }

    public static String sIsDataOnly(String sIN) {
        int iPosCR = sIN.indexOf("\r");
        if ((iPosCR >= 0)) {
            return sIN.substring(0, iPosCR);
        }

        return "";
    }

    public static class BarcodeData {

        public String honID;

        public String AimID;

        public String AimMod;

        public String sData;

        public BarcodeData(String sIDs) {
            this.honID = sIDs.substring(0, 1);
            this.AimID = sIDs.substring(1, 1);
            this.AimMod = sIDs.substring(2, 1);
            sData = "";
        }

        public String toString() {
            return sData;
        }
    }

    public static byte[] myGetBytes(String sIn) {
        try {
            return sIn.getBytes("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] setTriggerOnMsg() {
        //  SYN T CR
        return myGetBytes("\u0016T\r");
    }

    public static byte[] setTriggerOffMsg() {
        //  SYN U CR
        return myGetBytes("\u0016U\r");
    }

    public static byte[] setManualTriggerMode() {
        String s = "PAPHHF.";
        return myGetBytes(s);
    }

    public static byte[] setTriggerClickOn() {
        String s = "BEPTRG1.";
        return myGetBytes(s);
    }

    public static byte[] setTriggerClickOff() {
        String s = "BEPTRG0.";
        return myGetBytes(s);
    }

    public static byte[] setGoodReadBeepOff() {
        String s = (SynMenuCommandHeader + ("BEPBEP0" + SynMenuCommandSuffixFlash));
        return myGetBytes(s);
    }

    public static byte[] setGoodReadBeepOn() {
        String s = (SynMenuCommandHeader + ("BEPBEP1" + SynMenuCommandSuffixFlash));
        return myGetBytes(s);
    }

    public static byte[] getDecHeaderSetting() {
        String s = (SynMenuCommandHeader + ("DECHDR?" + SynMenuCommandSuffixFlash));
        return myGetBytes(s);
    }

    public static byte[] setDecHeaderSetting(boolean bOnOff) {
        String s;
        if (bOnOff)
            s = (SynMenuCommandHeader + ("DECHDR" + "1" + SynMenuCommandSuffixFlash));
        else
            s = (SynMenuCommandHeader + ("DECHDR" + "0" + SynMenuCommandSuffixFlash));
        return myGetBytes(s);
    }

    public static byte[] setDoBeep() {
        String s = (SynMenuCommandHeader + "BEPEXE1" + SynMenuCommandSuffixFlash);
        byte[] bytes=myGetBytes(s);
        return bytes;
    }

    public static byte[] SetAllSymbologiesOnOff(boolean bOnOff) {
        String s;
        if (bOnOff)
            s = (SynMenuCommandHeader + "ALLENA" + "1" + SynMenuCommandSuffixFlash);
        else
            s = (SynMenuCommandHeader + "ALLENA" + "0" + SynMenuCommandSuffixFlash);
        return myGetBytes(s);
    }

    static String deviceName = "";

    // or ":*:" or BT friendly name, ie ":8680i wearable:
    public static String SynMenuCommandHeader = ("\u0016"+ "M" +"\r" + deviceName);

    // :*: = name of device
    public static String SynYCommandHeader = ("\u0016Y\r" + deviceName); //0x0d = \r ???

    // :*: = name of device
    public static String SynMenuCommandSuffixFlash = ".";

    public static String SynMenuCommandSuffixRAM = "!";

    public static class eASCII_codes {
        static int NUL = 0;
        static int ACK = 6;
        static int CR = 19;
        static int NAK = 21;
        static int SYN = 22;
        static int GS = 29;
    }
}

