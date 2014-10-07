package edu.skku.monet.NAFrequencyService.Sender;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Message;
import edu.skku.monet.NAFrequencyService.*;

/**
 * 
 * This class contains methods to generate sampled short array data of sound.
 * 
 * @author Jeffrey
 *
 */
public class SignalGenerator {
	
	private static final int channel_config = AudioFormat.CHANNEL_OUT_STEREO;
	private static final int channel_format = AudioFormat.ENCODING_PCM_16BIT;
	private static final int sampleSize = 44100;
	
	public static boolean isRunning = true;
	/**
	 * This method returns simple sine wave with specific frequency and sampleRate.
	 * 
	 * @param freq Frequency by Hz. to make inaudible sound it should be between 18000 and 22000.
	 * @param sec Time how much this sound continues
	 * @param sampleRate sampling rate by Hz, normally 44100 or 48000
	 * @return sine wave sampled data array
	 */
	private static short[] createSinWaveBuffer(int freq, int sec, int sampleRate) {
       short[] output = new short[sec * sampleRate];

       double period = (double)sampleRate / freq;
       for (int i = 0; i < output.length; i++) {
           double angle = 2.0 * Math.PI * i / period;
           output[i] = (short)(Math.sin(angle) * 32767f / 40);  }

       return output;
	}
	
	/**
	 * This method returns complex or simple wave with specific frequency(frequency array if you want to make complex wave) and sampleRate.
	 * 
	 * @param freq Frequency by Hz. to make inaudible sound it should be between 18000 and 22000.
	 * @param sec Time how much this sound continues
	 * @param sampleRate sampling rate by Hz, normally 44100 or 48000
	 * @return complex wave sampled data array
	 */
	public static short[] createComplexWaveBuffer(int[] freq, int sec, int sampleRate){
		// 16bit arrange
        int realLength = 0;
		sec <<= 1;
		int sampleNum = sec * sampleRate;
		short[] output = new short[sampleNum];
		
		// 각 주파수에 대해 단일 주파수 사인파형을 생성
		short[][] sinwaveList = new short[freq.length][];
		for(int i=0; i<freq.length; i++){
            if(freq[i] != 0) {
                sinwaveList[realLength] = createSinWaveBuffer(freq[i], sec, sampleRate);
                realLength++;
            }
		}
		// 동일한 시간대의 샘플 데이터의 평균을 구하여 output 배열에 삽입
		for(int i=0; i<sampleNum; i++){
			long data = 0;
			
			for(int j=0; j<realLength; j++){
				data += sinwaveList[j][i];
			}
			//data /= freq.length;
			
			output[i] = (short)data;
		}
		
		return output;
	}

    public static int[] genSignal(byte[] signalInfo) {
        int freq[] = new int[40];

        for(int i = 0; i < signalInfo.length; i++) {
            if(signalInfo[i] == 1)
            {
                freq[i] = Constants.startFreq + (i * Constants.guardFreq);
            }
        }

        return freq;
    }
	
	public static void sendSignal(final int[] freq){
		new Thread(){
        	public void run() {
        		
        		Message m = new Message();
        		m.obj = new String[]{"Making soundwave data..", ""};
        		MyActivity.handler.sendMessage(m);
                int[] freqtmp = new int[32];
                /*
        		int[] freq = new int[15];
        		for(int i=0; i<15; i++){
        			freq[i] = 18050 + ((int)(Math.random() * 40)) * 100;
        		}
        		*/
        		
        		String str = "";

                /*
        		for(int i=0; i<15; i++){
        			for(int j=i; j<15; j++){
        				if(freq[i] > freq[j]){
        					int temp = freq[i];
        					freq[i] = freq[j];
        					freq[j] = temp;
        				}
        			}
        		}
        		*/
        		
        		for(int i=0; i<16; i++){
                    freqtmp[i] = 18000 + (i*100);
                    str += "1";
        		}
                for(int i = 0; i < 16; i++) {
                    freqtmp[15+i] = freq[i];
                    if(freq[i] != 0)
                        str += "1";
                    else
                        str += "0";
                }
        		
        		
        		short[] signal = SignalGenerator.createComplexWaveBuffer(freqtmp, 15, sampleSize);
        		final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleSize, channel_config, channel_format, signal.length, AudioTrack.MODE_STATIC);
                audioTrack.write(signal, 0, signal.length);
                audioTrack.play();

        		m = new Message();
        		m.obj = new String[]{"Sending Freqs : " + str, ""};
        		MyActivity.handler.sendMessage(m);
        	};
        }.start();
	}
}
