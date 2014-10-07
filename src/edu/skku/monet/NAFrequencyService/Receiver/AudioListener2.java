package edu.skku.monet.NAFrequencyService.Receiver;

import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Message;
import android.util.Log;
import edu.skku.monet.NAFrequencyService.MyActivity;

/**
 * 
 * This AudioListener records audio through mic,
 * and does fast fourier transform to get frequency range data.
 * FFT library figure out which frequencies this audio data contains,
 * and figure out the index of real data that audio want to send to user.
 * 
 * @author Jeffrey
 *
 */

public class AudioListener2{

	private static final int channel_config = AudioFormat.CHANNEL_IN_MONO;
	private static final int channel_format = AudioFormat.ENCODING_PCM_16BIT;
	private static final int sampleSize = 44100;
	private static int bufferSize = AudioRecord.getMinBufferSize(sampleSize, channel_config, channel_format);
	
	private AudioRecord audioRecord;
	private FFTManager fFTManager;
	
	private ListenerThread thread;
	
	private short[] buffer;
	double[] beforeTransformedDoubleData;
	double[] afterTransformedDoubleData;
	
	private boolean isCancelled = false;
	
	private static final int determineTryingNumber = 30;
	
	public AudioListener2(){
		// 버퍼사이즈를 2의 승수로 맞춘다.
		setBufferSizeToPowerOfTwo();
					
		// 마지막 parameter의 단위는 byte이고, 입력단위는 short이기 때문에 개수를 맞추기 위해 *2를 해줌.
		audioRecord = new AudioRecord(
				AudioSource.MIC,
				sampleSize,
				channel_config,
				channel_format,
				bufferSize << 1);
		
		double freqRes = (sampleSize >> 1) / (double)(bufferSize >> 1);
		Log.i("test", "frequency resolution : " + freqRes);
		
		fFTManager = new FFTManager(bufferSize, freqRes);
		
		buffer = new short[bufferSize];
		beforeTransformedDoubleData = new double[bufferSize];
		afterTransformedDoubleData = new double[bufferSize];
		
	}
	
	public synchronized void startListen(){
		thread = new ListenerThread();
		thread.start();
		isCancelled = false;
	}
	
	public synchronized void stopListen(){
		if(thread != null){
			isCancelled = true;
		}
			
	}
	
	/**
	 * bufferSize값을 2의 승수로 맞춘다.
	 * bufferSize값은 FFT알고리즘의 조건에 의해 데이터가 2의 승수가 되어야 하며, AudioRecord클래스의 정의에 따라
	 * {@link android.media.AudioRecord#getMinBufferSize(int, int, int)}보다 커야 한다.
	 */
	private void setBufferSizeToPowerOfTwo(){
		int tmp = 1;
		
		while(tmp < bufferSize) tmp <<= 1;
		
		bufferSize = tmp << 1;
		Log.i("test", "Buffer size : " + tmp);
	}
	
	private class ListenerThread extends Thread{
		/**
		 * 소리를 blockSize만큼 입력받고 FFT로 변환한다.
		 * 변환된 데이터는 {@link edu.skku.monet.NAFrequencyService.Receiver.AudioListener2#onProgressUpdate(Double[]...}
		 * 로 전송된다.
		 * 
		 */ 
		@Override
		public void run() {
			// TODO Auto-generated method stub2w
			
			audioRecord.startRecording();
			
			while(!isCancelled){
				
				Message m = new Message();
				m.obj = new String[]{"checking out whether there is sound every 3 secs..", ""};
				
				MyActivity.handler.sendMessage(m);
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				int readResult = audioRecord.read(buffer, 0, bufferSize);
				
				// byte로 읽은 buffer를 double로 변환
				for(int i=0; i<readResult; i++){
					beforeTransformedDoubleData[i] = buffer[i] / 2048.0;
				}
				
				// FFT를 수행
				afterTransformedDoubleData = fFTManager.transformFFT(beforeTransformedDoubleData);
			
				double[] frequencies = fFTManager.getFrequencies(afterTransformedDoubleData);
				
				Log.i("test", "frequency detection finished");
				
				if(frequencies.length > 0){
					m = new Message();
					m.obj = new String[]{"some sound is recognized", ""};
					
					MyActivity.handler.sendMessage(m);
					
					double[] frequencyList = determineFrequency(audioRecord);
					if(frequencyList.length > 0){
						String str = "end : ";
						for(int i=0; i<frequencyList.length; i++){
							str += frequencyList[i] + "Hz ";
						}
						
						byte[] bitValue = fFTManager.getDataValue(frequencyList);
						
						String str2 = "";
						for(int i=bitValue.length-1; i>=0; i--){
							str2 += bitValue[i];
						}
						m = new Message();
						m.obj = bitValue;
						MyActivity.handler.sendMessage(m);
						return;
					}
				}
			}
			
			/*
			while(!isCancelled){
				
				int readResult = audioRecord.read(buffer, 0, bufferSize);
				
				// byte로 읽은 buffer를 double로 변환
				for(int i=0; i<readResult; i++){
					beforeTransformedDoubleData[i] = buffer[i] / 1024.0;
				}
				
				// FFT를 수행
				afterTransformedDoubleData = fFTManager.transformFFT(beforeTransformedDoubleData);
			
				double[] frequencies = fFTManager.getFrequencies(afterTransformedDoubleData);
				
				
				String str = "frequency : ";
				
				for(int i=0; i<frequencies.length; i++){
					str += frequencies[i] + "hz ";
				}
				
				//Log.i("test", str);
				
				
				byte[] bitvalue = fFTManager.getDataValue(frequencies);
				
				String str2 = "";
				for(int i=bitvalue.length-1; i>=0; i--){
					str2 += bitvalue[i];
				}
				
				//Log.i("test2", str2);
				
				Message m = new Message();
				m.obj = new String[]{str, str2};
				
				MyActivity.handler.sendMessage(m);
				
			}*/
		}
	}
	/**
	 * 앱에서 처리하는 주파수가 발견될 경우 호출되는 메서드로써, 20번 연속으로 마이크로부터 입력을 받아 데이터가 계속 유지되는지를 확인한다.
	 * 
	 * @param audioRecord AudioRecord 인스턴스
	 * @return 실제로 포함되어 있다고 판단된 주파수 값의 배열
	 */
	public double[] determineFrequency(AudioRecord audioRecord){
		
		byte[] bitResult = new byte[fFTManager.getBitLength()];
		ArrayList<Double> frequencyList = new ArrayList<Double>();
		
		int i;
		for(i=0; i<determineTryingNumber; i++){
			int readResult = audioRecord.read(buffer, 0, bufferSize);
			
			for(int j=0; j<readResult; j++){
				beforeTransformedDoubleData[j] = buffer[j] / 2048.0;
			}
			
			afterTransformedDoubleData = fFTManager.transformFFT(beforeTransformedDoubleData);
			
			double[] frequencies = fFTManager.getFrequencies(afterTransformedDoubleData);
			
			String str = "";
			
			for(int j=0; j<frequencies.length; j++){
				str += (int)frequencies[j] + " ";
			}
			
			//Log.i("test", str);
			
			byte[] bitValue = fFTManager.getDataValue(frequencies);
			
			for(int j=0; j<bitValue.length; j++){
				if(bitValue[j] == 1) bitResult[j]++;
			}
		}
		
		// determineTryingNumber 중 60% 이상 검출되면 실제 데이터가 존재한다고 판단
		for(int j=0; j<bitResult.length; j++){
			if(bitResult[j] >= determineTryingNumber * 0.6f) frequencyList.add(fFTManager.getFrequencyValue(j));
		}
		
		double[] result = new double[frequencyList.size()];
		for(int j=0; j<result.length; j++){
			result[j] = frequencyList.get(j);
		}
		
		return result;
	}

}
