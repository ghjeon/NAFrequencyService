package edu.skku.monet.NAFrequencyService.Receiver;

import java.util.ArrayList;

import android.util.Log;
import ca.uol.aig.fftpack.RealDoubleFFT;

/**
 * 
 * This FFTManager class contains everything about FFT
 * 
 * @author Jeffrey
 *
 */


public class FFTManager {
	
	/**
	 * freqRes : FFT결과값 배열의 인덱스당 주파수
	 * 예를 들어, freqRes = 10Hz / bin이고 FFT결과값에서 인덱스가 100인 값은 1000Hz 소리 크기이다.
	 */
	private final double freqRes;
	
	/**
	 * minFrequency : 앱에 사용할 최저 주파수(Hz)
	 */
	private final int minFrequency = 18000;
	
	/**
	 * maxFrequency : 앱에 사용할 최고 주파수(Hz)
	 */
	private final int maxFrequency = 22000;
	
	/**
	 * frequencyDistance : 각 비트 간 주파수 간격. 100을 권장
	 */
	private final int frequencyDistance = 100;
	
	final RealDoubleFFT transformer;
	
	public FFTManager(int bufferSize, double freqRes){
		this.freqRes = freqRes;
		transformer = new RealDoubleFFT(bufferSize);
	}
	
	
	/**
	 * 
	 * 이 메서드는 FFT를 수행합니다. 
	 * 시간대역 그래프를 변환하여 주파수대역 그래프로 변환합니다.
	 * 허수와 실수의 크기를 합치는 {@link edu.skku.monet.NAFrequencyService.Receiver.FFTManager#getFFTValue(double[])}
	 * 함수를 내부에서 수행합니다.
	 * 
	 * 리턴하는 double 배열이 parameter크기의 절반인 이유는 FFT계수값의 실수부와 허수부를 합치는 과정에서 발생하는 현상으로,
	 * 아래 명시된 FFT라이브러리의 javadoc을 참조.
	 * {@link ca.uol.aig.fftpack.RealDoubleFFT#ft(double[])}
	 * 
	 * @param transformedData FFT를 수행할 시간대역 그래프 데이터로 더블형 데이터의 배열 
	 * @return FFT를 수행한 주파수 대역 그래프 데이터로 parameter 길이의 1/2의 더블형 데이터 배열
	 */
	
	public double[] transformFFT(double[] beforeTransformedData){
		transformer.ft(beforeTransformedData);
		
		double[] afterTransformedData = new double[(beforeTransformedData.length >> 1) + 1];
		
		// real
		afterTransformedData[0] = beforeTransformedData[0];
		afterTransformedData[afterTransformedData.length - 1] = beforeTransformedData[beforeTransformedData.length - 1];
		
		// real + imaginary
		for(int i=1; i<afterTransformedData.length - 1; i++){
			int i2 = i << 1;
			double rl = beforeTransformedData[i2-1];
			double im = beforeTransformedData[i2];
			
			afterTransformedData[i] = Math.sqrt(rl * rl + im * im);
		}
		
		return afterTransformedData;
	}
	
	/**
	 * 이 메서드는 FFT변환된 데이터에서 유의미한 주파수 인덱스를 반환합니다.
	 * 해당 오디오 파형에서 어떤 주파수 대역의 소리가 실제로 포함되어있는지를 판단하고 해당 주파수 인덱스의 배열을 리턴합니다.
	 * 
	 * @param transfomedData {@link edu.skku.monet.NAFrequencyService.Receiver.FFTManager#transformFFT(double[])}
	 * 로 변환된 FFT 주파수 배열
	 * @return parameter의 주파수 배열에서 실제 음성 파형에 포함되어 있다고 판단되는 주파수의 배열 인덱스의 배열
	 */
	
	public double[] getFrequencies(double[] transformedData){

		double min;
		
		// 최소 주파수 위로만 확인
		int minIndex = (int)(minFrequency / freqRes) + 1;
		
		ArrayList<Double> resultlist = new ArrayList<Double>();
		
		
		String str = "";
		
		for(int i=minIndex; i<transformedData.length; i++){
			double freq = i * freqRes;
			//min = 50 - 47 * Math.log10(((freq / 200) - 88) / 2);
			
			min = 18 - (freq - minFrequency) / (double)240;
			
			if(freq > maxFrequency) continue;
			if(transformedData[i] >= min) {
				resultlist.add(freq);
				str += (int)freq + " : " + transformedData[i] + " / " + min + "\n";
			}
		}
		
		Log.i("test", str);
		
		// ArrayList<Integer>를 int[]로 변환
		// toArray함수로는 Integer[]로 변환되고 int[]로 변환할 수 없음
		double[] result = new double[resultlist.size()];
		int i=0;
		
		
		for(Double data : resultlist){
			result[i++] = data;
		}
		
		return result;
	}
	
	/**
	 * 이 메서드는 {@link edu.skku.monet.NAFrequencyService.Receiver.FFTManager#getFrequencies(double[])}의 결과값을 parameter로 받아
	 * 데이터 인덱스를 추출하는 메서드입니다.
	 * @param frequencies {@link edu.skku.monet.NAFrequencyService.Receiver.FFTManager#getFrequencies(double[])}의 리턴값인 더블형 배열
	 * @return 데이터 인덱스
	 */
	public byte[] getDataValue(double[] frequencies){
		
		byte[] value = new byte[getBitLength()];
		
		for(int i=0; i<frequencies.length; i++){
			// 비트값 추출
			int bitValue = ((int)(frequencies[i] - minFrequency) / frequencyDistance);
			if(bitValue >= value.length || bitValue < 0) continue;
			value[bitValue] = 1;
		}
		
		return value;
	}
	
	/**
	 * 이 메서드는 비트 인덱스를 받아 해당하는 주파수를 반환합니다.
	 * @param bitIndex 비트 인덱스 값.
	 * @return 해당 주파수 값.
	 */
	public double getFrequencyValue(int bitIndex){
		return minFrequency + (bitIndex + 0.5) * frequencyDistance;
	}
	
	/**
	 * 이 메서드는 해당 설정에서 소리를 이용한 데이터의 비트 개수를 반환합니다.
	 * @return 데이터 비트 길이
	 */
	public int getBitLength(){
		return (maxFrequency - minFrequency) / frequencyDistance;
	}
		
}
