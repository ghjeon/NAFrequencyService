package edu.skku.monet.NAFrequencyService;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by Gyuhyeon on 2014. 10. 5..
 */
public class Constants {

    /* Device IDs */

    public static final int startFreq = 20000;
    public static final int guardFreq = 100;
    public static final int endFreq = 22000;

    static List<String> deviceIdSet = new ArrayList<String>();

    static final int USER = 0x0000;
    static final int N = 0x1000;
    static final int E = 0x0100;
    static final int W = 0x0010;
    static final int S = 0x0001;
    static final int NE = 0x1100;
    static final int NW = 0x1010;
    static final int SE = 0x0110;
    static final int SW = 0x0011;

    /* Command IDs */

    static List<String> commandIdSet = new ArrayList<String>();

    static final int WAIT = 0x0000;
    static final int LISTEN = 0x1000;
    static final int ERROR = 0x1111;
    static final int RETRY = 0x1110;
    static final int REQUEST = 0x0001;
    static final int OK = 0x0010;
    static final int NOK = 0x1010;

    static Map<String, Integer> DeviceIds = new HashMap<String, Integer>();
    static Map<String, Integer> CommandIds = new HashMap<String, Integer>();


    static final int thisDevice = S;


    static {
        deviceIdSet.add("N");
        deviceIdSet.add("E");
        deviceIdSet.add("W");
        deviceIdSet.add("S");
        deviceIdSet.add("NE");
        deviceIdSet.add("NW");
        deviceIdSet.add("SE");
        deviceIdSet.add("SW");

        DeviceIds.put("USER", USER);
        DeviceIds.put("N", N);
        DeviceIds.put("E", E);
        DeviceIds.put("W", W);
        DeviceIds.put("S", S);
        DeviceIds.put("NE", NE);
        DeviceIds.put("NW", NW);
        DeviceIds.put("SE", SE);
        DeviceIds.put("SW", SW);

        commandIdSet.add("WAIT");
        commandIdSet.add("LISTEN");
        commandIdSet.add("ERROR");
        commandIdSet.add("RETRY");
        commandIdSet.add("REQUEST");
        commandIdSet.add("OK");
        commandIdSet.add("NOK");

        CommandIds.put("WAIT", WAIT);
        CommandIds.put("LISTEN", LISTEN);
        CommandIds.put("ERROR", ERROR);
        CommandIds.put("RETRY", RETRY);
        CommandIds.put("REQUEST", REQUEST);
        CommandIds.put("OK", OK);
        CommandIds.put("NOK", NOK);
    }

    public static byte[] setXOR(byte[] sigInfo) {
        sigInfo[12] = (byte)(sigInfo[0] ^ sigInfo[4] ^ sigInfo[8]);
        sigInfo[13] = (byte)(sigInfo[1] ^ sigInfo[5] ^ sigInfo[9]);
        sigInfo[14] = (byte)(sigInfo[2] ^ sigInfo[6] ^ sigInfo[10]);
        sigInfo[15] = (byte)(sigInfo[3] ^ sigInfo[7] ^ sigInfo[11]);
        return sigInfo;
    }

    public static byte[] setCommand(byte[] sigInfo, String comm) {
        int command = Constants.CommandIds.get(comm);
        byte[] commandBytes = ByteBuffer.allocate(4).putInt(command).array();
        sigInfo[4] = sigInfo[0];
        sigInfo[5] = sigInfo[1];
        sigInfo[6] = sigInfo[2];
        sigInfo[7] = sigInfo[3];
        return sigInfo;
    }

    public static byte[] genSignal(byte[] source, byte[] target) {
        byte[] signalInfo = new byte[16];
        signalInfo[0] = target[0];
        signalInfo[1] = target[1];
        signalInfo[2] = target[2];
        signalInfo[3] = target[3];
        signalInfo[8] = source[0];
        signalInfo[9] = source[1];
        signalInfo[10] = source[2];
        signalInfo[11] = source[3];
        return signalInfo;
    }

}
