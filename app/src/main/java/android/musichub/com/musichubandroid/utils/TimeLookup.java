package android.musichub.com.musichubandroid.utils;

// List of time servers: http://tf.nist.gov/service/time-servers.html



import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.util.Date;
//import org.apache.commons.net.time.TimeTCPClient;
//import org.apache.commons.net.time.TimeUDPClient;


public class TimeLookup {
	public long offset = 0;
	public static final String TIME_SERVER = "time-a.nist.gov";

	public TimeLookup() throws Exception{
		// Global Time
		NTPUDPClient timeClient = new NTPUDPClient();
		InetAddress inetAddress = null;
		TimeInfo timeInfo = null;
		this.offset = 0;
		//try {
			inetAddress = InetAddress.getByName(TIME_SERVER);
			timeInfo = timeClient.getTime(inetAddress);

			long returnTime = timeInfo.getReturnTime();
			Date time = new Date(returnTime);
			long globalSinceMidnight = time.getTime() % (24 * 60 * 60 * 1000);
			// System.out.println("Time from " + TIME_SERVER + ": " + time);
			// System.out.println(returnTime);
			//System.out.println("globalSinceMidnight: " + globalSinceMidnight);

			// Local time
			Date localTime = new Date();
			long localSinceMidnight = localTime.getTime() % (24 * 60 * 60 * 1000);
			//System.out.println("localSinceMidnight: " + localSinceMidnight);

			this.offset = globalSinceMidnight - localSinceMidnight;
		//}
//		catch (UnknownHostException e) {
//
//			e.printStackTrace();
//			//this.offset = offset;
//		} catch (IOException e) {
//
//			e.printStackTrace();
//			//return offset;
//		}
		
		
	}
	
	public TimeLookup(long ServerTimeSinceMidnight){

		Date localTime = new Date();
		long localSinceMidnight = localTime.getTime() % (24 * 60 * 60 * 1000);
		this.offset = ServerTimeSinceMidnight - localSinceMidnight;
	}
	
	public long getCurrentTime(){
		Date localTime = new Date();
		long localSinceMidnight = localTime.getTime() % (24 * 60 * 60 * 1000);
		return localSinceMidnight + offset;
	}

	public void adjustOffset(long adjustOffset){
		this.offset += adjustOffset;
	}

	public static void main(String[] args) throws Exception {
		// Global Time
//		NTPUDPClient timeClient = new NTPUDPClient();
//		InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
//		TimeInfo timeInfo = timeClient.getTime(inetAddress);
//		long returnTime = timeInfo.getReturnTime();
//		Date time = new Date(returnTime);
//		long globalSinceMidnight = time.getTime() % (24 * 60 * 60 * 1000);
//		// System.out.println("Time from " + TIME_SERVER + ": " + time);
//		// System.out.println(returnTime);
//		System.out.println("globalSinceMidnight: " + globalSinceMidnight);
//
//		// Local time
		Date localTime = new Date();
		long localSinceMidnight = localTime.getTime() % (24 * 60 * 60 * 1000);
		System.out.println("localSinceMidnight: " + localSinceMidnight);
//
//		long offset = globalSinceMidnight - localSinceMidnight;
		// System.out.println(new Date());
		// Calendar rightNow = Calendar.getInstance();
		//
		// // offset to add since we're not UTC
		// long offset = rightNow.get(Calendar.ZONE_OFFSET) +
		// rightNow.get(Calendar.DST_OFFSET);
		// long sinceMidnight = (rightNow.getTimeInMillis() + offset) %
		// (24 * 60 * 60 * 1000);
		//
		// System.out.println(sinceMidnight + " milliseconds since midnight");
	}
}