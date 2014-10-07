package edu.skku.monet.NAFrequencyService;

import android.content.Context;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;
import edu.skku.monet.NAFrequencyService.Receiver.AudioListener2;
import edu.skku.monet.NAFrequencyService.Sender.SignalGenerator;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Gyuhyeon
 * Date: 2014. 9. 14.
 * Time: 오전 11:16
 * To change this template use File | Settings | File Templates.
 */
public class NAFService extends Service {

    private AudioListener2 listener;
    public static FFTHandler handler;

    public static int lastState = 0;
    public static int retry = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if(listener == null)
            listener = new AudioListener2();
        if(handler == null)
            handler = new FFTHandler();
        super.onCreate();
    }

    public void onDestory() {
        listener.stopListen();
        listener = null;
        handler = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        listener.startListen();
        return START_STICKY;
        //super.onStartCommand(intent, flags, startId);
    }

    public class FFTHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            byte[] str = (byte[]) msg.obj;
            byte[] source = new byte[4];
            byte[] command = new byte[4];
            byte[] target = new byte[4];
            byte[] xor = new byte[4];
            int[] xorResult = new int[4];

            for(int i = 0; i < 4; i++) {
                source[i] = str[i];
                command[i] = str[4 + i];
                target[i] = str[8 + i];
                xor[i] = str[12 + i];
                xorResult[i] = source[i] ^ command[i] ^ target[i];
                if(xor[i] != xorResult[i])
                    return;
            }

            if(Constants.thisDevice == ByteBuffer.wrap(target).getInt())
            {
                listener.stopListen();
                int c = ByteBuffer.wrap(command).getInt();
                byte[] sigInfo = Constants.genSignal(source, target);
                switch(c) {
                    case Constants.LISTEN :
                        //디바이스 대기중
                        break;
                    case Constants.ERROR :
                        retry = 0;
                        //디바이스 이상. 관련 내용 서버 기록. 사용자 피드백.
                        break;
                    case Constants.REQUEST :
                        String sig = "";
                        retry = 0;
                        if(new Random().nextInt(10) % 2 > 5)
                            sig = "OK";
                        else
                            sig = "NOK";
                        sigInfo = Constants.setCommand(sigInfo, sig);
                        sigInfo = Constants.setXOR(sigInfo);
                        SignalGenerator.sendSignal(SignalGenerator.genSignal(sigInfo));
                        //요청 받음. 신호 관련 처리해야
                       break;
                    case Constants.OK :
                        retry = 0;
                        //동작 성공. 관련 내용 서버 기록
                        break;
                    case Constants.NOK :
                        retry = 0;
                        //동작 실패. 관련 내용 서버 기록
                        break;
                    case Constants.RETRY :
                        //요청 보내야 함
                        if(retry > 10)
                            break;
                        retry++;
                    case Constants.WAIT :
                        sigInfo = Constants.setCommand(sigInfo, "REQUEST");
                        sigInfo = Constants.setXOR(sigInfo);
                        SignalGenerator.sendSignal(SignalGenerator.genSignal(sigInfo));
                        //요청 보내야 함
                        break;
                    default: break;
                }
                lastState = c;
            }
        }

    }
}
