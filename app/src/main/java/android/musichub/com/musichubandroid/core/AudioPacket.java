package android.musichub.com.musichubandroid.core;


public class AudioPacket {
	int length;
	byte[] packet = null;
	long playTime;
	
	public AudioPacket(int length, byte[] packet, long playTime){
		this.length = length;
		this.packet = packet;
		this.playTime = playTime;
	}
	
	public String toString(){
		return "Packet Length : "+Integer.toString(length);
	}
	
	public int getSize(){
		if(packet == null) return 0;
		else return packet.length;
	}
	
	public int cutPacketByTime(long milliSecond, int frameSize, int frameRate){
		
		return 0;
	}
}
