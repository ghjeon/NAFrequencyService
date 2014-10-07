package edu.skku.monet.NAFrequencyService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.skku.monet.NAFrequencyService.Receiver.AudioListener2;
import edu.skku.monet.NAFrequencyService.Sender.SignalGenerator;

import java.nio.ByteBuffer;

public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private Intent serviceIntent;

    private String deviceId = "S";
    private Integer deviceCode = Constants.DeviceIds.get(deviceId);


    private button serviceButton = null;
    private changeIdButton idButton = null;
    private receiveButton receiveButton = null;

    private static TextView text = null;
    private static TextView text2 = null;
    public static boolean started = false;
    public static AudioListener2 listener;
    public static FFTHandler handler;
    
    class button extends Button {
        /* PlayButton의 기능 정의. Button 으로부터 상속받아 구현됨 */

        boolean mStartPlaying = true; // 현재 레코딩 상황을 보관함

        /* EventListener. Button Object에 대한 onClick Event, 즉 사용자가 버튼을 눌렀을 경우에 관한 Event를 통제하는 Listener. */

        byte[] sig = new byte[16];
        byte[] sourceId = ByteBuffer.allocate(4).putInt(deviceCode).array();
        byte[] targetId = ByteBuffer.allocate(4).putInt(Constants.DeviceIds.get("USER")).array();


        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                sig = Constants.genSignal(targetId, sourceId);
                sig = Constants.setCommand(sig, "WAIT");
                sig = Constants.setXOR(sig);
                SignalGenerator.sendSignal(SignalGenerator.genSignal(sig));
            }
        };

        public button(Context ctx) {  // Play Button Constructor
            super(ctx);
            setText("Create Signal Start");
            setOnClickListener(clicker);
        }
    }

    class changeIdButton extends Button {

        OnClickListener clicker =  new OnClickListener() {
            public void onClick(View v) {
                changeId();
            }
        };

        public changeIdButton(Context ctx) {
            super(ctx);
            setText(deviceId);
            setOnClickListener(clicker);
        }

    }

    class receiveButton extends Button {

        OnClickListener clicker =  new OnClickListener() {
            public void onClick(View v) {
                startService(serviceIntent); //receiveSignal();
            }
        };

        public receiveButton(Context ctx) {
            super(ctx);
            setText("Signal Receive Service Start");
            setOnClickListener(clicker);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serviceIntent = new Intent(this, NAFService.class);
        
        handler = new FFTHandler();
        
        LinearLayout ll = new LinearLayout(this); // 레이아웃 종류 선택
        ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        ll.setOrientation(LinearLayout.VERTICAL);
        serviceButton = new button(this);  // 레코드 버튼 객체 생성
        receiveButton = new receiveButton(this);
        idButton = new changeIdButton(this);
        text = new TextView(this);
        text.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 120));
        text2 = new TextView(this);
        text2.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        ll.addView(serviceButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0)); // 앞서 선안한 레이아웃에 버튼 추가함
        ll.addView(receiveButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0)); // 앞서 선안한 레이아웃에 버튼 추가함
        ll.addView(text);
        ll.addView(text2);
        ll.addView(idButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0)); // 앞서 선안한 레이아웃에 버튼 추가함
        
        
        setContentView(ll); // 어플리케이션 화면에 레이아웃 출력

    }

    private void receiveSignal()
    {
        if(listener == null) listener = new AudioListener2();
        listener.startListen();
    }

    private void changeId()
    {
        int current = Constants.deviceIdSet.indexOf(deviceId) + 1;
        if(current == Constants.deviceIdSet.size())
            current = 0;
        deviceId = Constants.deviceIdSet.get(current);
        deviceCode = Constants.DeviceIds.get(deviceId);
        idButton.setText(deviceId);

    }
    
    public static class FFTHandler extends Handler{
    	@Override
    	public void handleMessage(Message msg) {

    		String[] str = (String[]) msg.obj;
    		text.setText(str[0]);
    		text2.setText(str[1]);
		}
    }
}
