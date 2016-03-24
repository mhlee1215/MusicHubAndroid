package android.musichub.com.musichubandroid.core;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.musichub.com.musichubandroid.MainActivity;
import android.musichub.com.musichubandroid.utils.ThreadController;
import android.musichub.com.musichubandroid.utils.TimeLookup;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CapitalizeClient {

	public static final int CONNECT_INIT = 999;
	public static final int CONNECT_FAIL = 998;
	public static final int CONNECT_SUCCESS = 997;

	public static final int DISCONNECT_INIT = 996;
	public static final int DISCONNECT_FAIL = 995;
	public static final int DISCONNECT_SUCCESS = 994;

	DataInputStream in = null;//new DataInputStream(socket.getInputStream());
	DataOutputStream out = null;
	ReceiveDaemon receiveDaemon = null;
	static PlayDaemon playDaemon = null;
	static Queue<AudioPacket> packets = null;
	static TimeLookup timeLookup = null;
	boolean play;
	boolean connected;
	boolean isSuspended = false;
	String clientName = "123";
	String serverAddress = "";




	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public void setPlay(boolean play) {
		this.play = play;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public void setVolume(int volume){
		System.out.println("[[[[SET VOLUME :"+volume);
//		SourceDataLine line = playDaemon.getDataLine();
//		FloatControl control=(FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
//		Control[] controls = line.getControls();
//		for(int i = 0 ; i < controls.length ; i++){
//			System.out.println(controls[i]);
//		}
//		float f_volume = ((float)volume);///100;
//		System.out.println("[[[[SET VOLUME :"+control.getMaximum());
//
//		float gap = (control.getMaximum() - control.getMaximum())*f_volume;
//		float v = control.getMinimum()+gap;
//
//		control.setValue(v);
	}

	public static void setVolume(float f_volume){
//		SourceDataLine line = playDaemon.getDataLine();
//		FloatControl control=(FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
//		float gap = (control.getMaximum() - control.getMinimum())*f_volume;
//		float v = control.getMinimum()+gap;
//		System.out.println("actual v:"+v);
//		System.out.println("control.getMinimum():"+control.getMinimum());
//		System.out.println("control.getMaximum():"+control.getMaximum());

//		control.setValue(v);
	}

	public CapitalizeClient(String clientName) {
		this();
		this.clientName = clientName;

		//timeLookup = new TimeLookup();
	}

	public CapitalizeClient() {
		play = false;
		connected = false;
		packets = new LinkedList<AudioPacket>();


		//timeLookup = new TimeLookup();
	}




	public boolean isPlay() {
		return play;
	}

	public boolean isConnected() {
		return connected;
	}




	public static class PlayDaemon extends ThreadController {


		AudioTrack sourceDataLine = null;
		long packetDuration = 0;
		int threshold;
		MeanClass gapMeanClass, reMeanClass, pGapMeanClass;
		int meanSize = 5;

		public PlayDaemon(AudioTrack sourceDataLine, long packetDuration, int threshold){
			this.sourceDataLine = sourceDataLine;

			//sourceDataLine.getControl(FloatControl.Type.VOLUME);
//			FloatControl control=(FloatControl)sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
//			control.setValue((float) 1);

			this.packetDuration = packetDuration;
			this.threshold = threshold;

			gapMeanClass = new MeanClass(meanSize);
			reMeanClass = new MeanClass(meanSize);
			pGapMeanClass = new MeanClass(meanSize);
		}

		public AudioTrack getDataLine(){
			return sourceDataLine;
		}

		@Override
		public void run() {
			while (!isFinished()) {
				synchronized (getLock()) {
					while (isPaused()) {
						try {
							getLock().wait();
						} catch (InterruptedException e) {
						}
					}
				}
				log("PlayDaemon, Playing...");
				if (packets.size() == 0){
					try {
						Thread.sleep(100);
						continue;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						log("PlayDaemon, No packet to play.. wait..");
					}
				}

				AudioPacket curPacket = packets.poll();

				long curTime = timeLookup.getCurrentTime();
				int gap = (int) (curPacket.playTime - curTime);

				//Packet is already passed then skip the packet
				if (gap < 0) continue;

						byte[] packetSyn = curPacket.packet;
				int expPacketSize = curPacket.length;

				//Wait until it's timing
				try {
					Thread.sleep(gap);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				sourceDataLine.write(packetSyn, 0, expPacketSize);
			}
		}



	}


	public static class MeanClass {
		public int SIZE = 5;
		List<Integer> list;
		int nextPointer = 0;

		public MeanClass(int size){
			this.SIZE = size;
			list = new ArrayList<Integer>(SIZE);
			for(int i = 0 ; i < SIZE; i++)
				list.add(0);
		}

		public MeanClass(){
			list = new ArrayList<Integer>(SIZE);
			for(int i = 0 ; i < SIZE; i++)
				list.add(0);
		}


		public float mean(int newData){
			list.set(nextPointer, newData);
			nextPointer++;
			if(nextPointer == SIZE) nextPointer = 0;

			return mean();
		}

		public float mean(){
			int meanData = 0;

			int counted = 0;
			for(int i = 0 ; i < list.size(); i++){
				if(list.get(i) != 0){
					meanData += list.get(i);
					counted++;
				}
			}
			return meanData / (float)counted;
		}
	}






	public static class ReceiveDaemon extends ThreadController {

		Socket socket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		AudioTrack sourceDataLine = null;
		int bufferedCycle = 0;
		MeanClass signalMeanClass;

		boolean isInit = false;

		//Init values
		float sampleRate;
		int bits;
		int channels;
		boolean isBigEndian;
		long packetDuration;
		long severTime;
		String clientName;
		int threshold;
		int packetSize;

		public ReceiveDaemon(Socket socket, String clientName, DataInputStream in, DataOutputStream out, int bufferedCycle) {
			this.socket = socket;
			this.in = in;
			this.out = out;
			this.bufferedCycle = bufferedCycle;
			signalMeanClass = new MeanClass();

			try {
				if(!isInit){

					Log.i(MainActivity.TAG, "client Name :"+clientName);
					out.writeUTF(clientName);
					out.writeInt(-1000);

					sampleRate = in.readFloat();
					bits = in.readInt();
					channels = in.readInt();
					isBigEndian = in.readBoolean();
					packetDuration = in.readLong();
					severTime = in.readLong();
					timeLookup = new TimeLookup(severTime);
					log("TEST!!! :"+timeLookup.offset);
					log("TEST!!! :"+timeLookup.getCurrentTime());

					int responseTimeCheckCount = in.readInt();
					long intervalMean = 0;
					for(int ii = 0 ; ii < responseTimeCheckCount ; ii++){
						long serverTime2 = in.readLong();
						intervalMean += timeLookup.getCurrentTime() - serverTime2;
					}
					timeLookup.adjustOffset(intervalMean/responseTimeCheckCount);
					//timeLookup.adjustOffset(210);
					//log("Receive Daemon, intervalMean:"+intervalMean);
					threshold = in.readInt();
					packetSize = in.readInt();

					isInit = true;
				}

				AudioTrack mAudioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC,
						(int)sampleRate,
						AudioFormat.CHANNEL_OUT_STEREO,
						AudioFormat.ENCODING_PCM_16BIT,
						packetSize,
						AudioTrack.MODE_STREAM);

				mAudioTrack.play();

				log("<<<<<<Audio Format!\n");
				log(mAudioTrack.toString());
//				DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat2);
//				sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
//				sourceDataLine.open(audioFormat2);
//				sourceDataLine.start();
				playDaemon = new PlayDaemon(mAudioTrack, packetDuration, threshold);
				log("PlayDaemon Started.\n");
			} catch(IOException e){
				e.printStackTrace();
				return;

			} catch(IllegalArgumentException e){
				e.printStackTrace();
				if(sourceDataLine != null)
					//sourceDataLine.close();
					try {
						socket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				return;
			}
//			catch (LineUnavailableException e) {
//
//				e.printStackTrace();
//				return;
//			}
		}

		@Override
		public void run() {

			//int i = 0;
			int errCount = 0;
			byte[] message = null;//new byte[packetSize];//new byte[length];
			//byte[] message2 = null;//new byte[packetSize];;//new byte[byteRead];
			while (!isFinished()) {
				synchronized (getLock()) {
					while (isPaused()) {
						try {
							getLock().wait();
						} catch (InterruptedException e) {
						}
					}
				}

				try {
					int byteRead = in.readInt();
					int length = in.readInt();



					log("ReceiveDaemon, byteRead:"+byteRead+", length:"+length+", packetSize:"+packetSize);

					if(byteRead < 0 || length < 0 || byteRead > packetSize*10 || length > packetSize*10){
						byteRead = 0;
						length = 0;
					}
					if(length>0) {
						long playTime = in.readLong();

//						if(message == null || message.length < length)
						message = new byte[packetSize];
//						if(message2 == null || message2.length < byteRead)
						//message2 = new byte[packetSize];

						log("ReceiveDaemon, receive length!! : "+length);
						in.readFully(message, 0, packetSize); // read the message

						int newSignal = -1000;//WifiDetector.getSignal();
						float meanSignal = signalMeanClass.mean(newSignal);
						out.writeInt(Math.round(meanSignal));


						//System.arraycopy(message, 0, message2, 0, packetSize);

						packets.add(new AudioPacket(byteRead, message, playTime));

						if(!playDaemon.isAlive()){
							if(packets.size() > bufferedCycle){
								log("ReceiveDaemon, Play Daemon started..");
								playDaemon.start();
							}
						}
					}else{
						System.err.println("ReceiveDaemon, Receive error");
						if(!playDaemon.isAlive()){
							playDaemon.start();
						}
						//break;
					}

				} catch (Exception ex) {
					//response = "Error: " + ex;
					ex.printStackTrace();
					System.err.println("Err to receive file");
					errCount++;
					if(errCount > 10){
						System.err.println("Client Ends");
						break;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}



	public int connectToServer(String serverAddress) throws IOException, InterruptedException {
		int connectStatus = CONNECT_INIT;
		this.serverAddress = serverAddress;
		log("connectToServer:"+serverAddress);
		int timeGapBetweenFail = 1000;
		int trial = 5;
		while (trial-- > 0) {
			try {
				log("Trying to connect server..");
				Socket socket = new Socket(serverAddress, 9898);
				log("Trying to get inputstream..");
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
				connected = true;
				play(socket, in, out);
				connectStatus = CONNECT_SUCCESS;
				break;
			} catch (ConnectException e) {
				System.err.println("Connection Fail.. try again after "+timeGapBetweenFail+"ms");
				connected = false;
				connectStatus = CONNECT_FAIL;
				//e.printStackTrace();
				Thread.sleep(timeGapBetweenFail);
			}
		}

		return connectStatus;
	}

	public void play(Socket socket, DataInputStream in, DataOutputStream out){
		if (receiveDaemon == null)
			receiveDaemon = new ReceiveDaemon(socket, clientName, in, out, 1);
		//receiveDaemon.setDaemon(true);
		log("Receive Daemon started..");
		receiveDaemon.start();
		play = true;
	}

//	public synchronized void resumePlay(){
//		log("resumePlay");
//		//receiveDaemon.notify();
//		//playDaemon.notify();
//		playDaemon.start();
//	}

	public void resume(){
		log("resume");
		if(isSuspended) {
			playDaemon.onResume();
			isSuspended = false;
		}
	}

	public void suspend(){
		log("suspend");
		isSuspended = true;
//		if(receiveDaemon!=null)
//			receiveDaemon.stop();
		if(playDaemon != null)
			playDaemon.onPause();

//		try {
//			receiveDaemon.socket.close();
//		} catch (IOException e) {
//
//			e.printStackTrace();
//		}
		play = false;
//		connected = false;
	}

	public synchronized int disconnectToServer(){
		int disconnectStatus = DISCONNECT_INIT;
		log("disconnectToServer");
		if(receiveDaemon!=null)
			receiveDaemon.onStop();
		if(playDaemon != null)
			playDaemon.onStop();
		try {
			receiveDaemon.socket.close();
			play = false;
			connected = false;
			disconnectStatus = DISCONNECT_SUCCESS;
		} catch (IOException e) {
			e.printStackTrace();
			disconnectStatus = DISCONNECT_FAIL;
		}

		return disconnectStatus;
	}


	public static void main(String[] args) throws Exception {

		log("I am client");
		CapitalizeClient client = new CapitalizeClient();
		String serverIP = "";
		if (args.length < 3)
			serverIP = "localhost";
		else serverIP = args[3];

		client.connectToServer(serverIP);
	}

	private static void log(String message) {
		Log.i(MainActivity.TAG, "[CLIENT] " + message);
	}

}
