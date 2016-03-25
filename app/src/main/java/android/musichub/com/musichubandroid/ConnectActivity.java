package android.musichub.com.musichubandroid;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.musichub.com.musichubandroid.core.CapitalizeClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

//import java.io.InputStream;
//import java.net.URL;

public class ConnectActivity extends AppCompatActivity {
    public final String LOG_TAG = "AUDIO_PLAY_EXAMPLE";
    public static final String TAG = "AUDIO_PLAY_EXAMPLE";

    ClientTask playTask;
    CapitalizeClient client;

    private static int TIMEOUT_US = -1;
    Thread t;
    int sr = 44100;
    boolean isRunning = false;
    //SeekBar fSlider;
    //double sliderval;
    //boolean sawInputEOS;

//    MediaExtractor extractor;
//    MediaFormat format;

    private class PlayerThread extends Thread {

        @Override
        public void run() {
            MediaExtractor extractor;
            MediaCodec codec;
            ByteBuffer[] codecInputBuffers;
            ByteBuffer[] codecOutputBuffers;

            AudioTrack mAudioTrack;

            mAudioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    44100,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    8192 * 2,
                    AudioTrack.MODE_STREAM);

            extractor = new MediaExtractor();
            try
            {
                extractor.setDataSource("http://www.ics.uci.edu/~minhaenl/data/timetolove.wav");
                MediaFormat format = extractor.getTrackFormat(0);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, String.format("MIME TYPE: %s", mime));

                codec = MediaCodec.createDecoderByType(mime);
                codec.configure(
                        format,
                        null /* surface */,
                        null /* crypto */,
                        0 /* flags */ );
                codec.start();
                codecInputBuffers = codec.getInputBuffers();
                codecOutputBuffers = codec.getOutputBuffers();

                extractor.selectTrack(0); // <= You must select a track. You will read samples from the media from this track!

                boolean sawInputEOS = false;
                boolean sawOutputEOS = false;

                for (;;) {
                    int inputBufIndex = codec.dequeueInputBuffer(-1);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                        int sampleSize = extractor.readSampleData(dstBuf, 0);
                        long presentationTimeUs = 0;
                        if (sampleSize < 0) {
                            sawInputEOS = true;
                            sampleSize = 0;
                        } else {
                            presentationTimeUs = extractor.getSampleTime();
                        }

                        codec.queueInputBuffer(inputBufIndex,
                                0, //offset
                                sampleSize,
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                        if (!sawInputEOS) {
                            extractor.advance();
                        }

                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        final int res = codec.dequeueOutputBuffer(info, -1);
                        if (res >= 0) {
                            int outputBufIndex = res;
                            ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                            final byte[] chunk = new byte[info.size];
                            buf.get(chunk); // Read the buffer all at once
                            buf.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN

                            mAudioTrack.play();

                            if (chunk.length > 0) {
                                mAudioTrack.write(chunk, 0, chunk.length);
                            }
                            codec.releaseOutputBuffer(outputBufIndex, false /* render */);

                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                sawOutputEOS = true;
                            }
                        }
                        else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
                        {
                            codecOutputBuffers = codec.getOutputBuffers();
                        }
                        else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
                        {
                            final MediaFormat oformat = codec.getOutputFormat();
                            Log.d(TAG, "Output format has changed to " + oformat);
                            mAudioTrack.setPlaybackRate(oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                        }
                    }
                }

            }
            catch (IOException e)
            {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        int workStatus;
        @Override
        protected Void doInBackground(String... params) {
            //showLoadingPanel();

            String myIP = null;
            try {
                myIP = Inet4Address.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            try {
                String type = params[1];
                if(type == null) type = "start";

                if("start".equalsIgnoreCase(type)){
                    String sAddress = params[0];
                    Log.i(TAG, "address : " + sAddress);
                    client = new CapitalizeClient();

                    workStatus = client.connectToServer(sAddress);
                }else if("stop".equalsIgnoreCase(type)){
                    //client = new CapitalizeClient();
                    workStatus = client.disconnectToServer();
                }else if("pause".equalsIgnoreCase(type)){
                    client.suspend();
                }else if("resume".equalsIgnoreCase(type)){
                    client.resume();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoadingPanel(workStatus);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            removeLoadingPanel(workStatus);
        }
    }

    public void showLoadingPanel(int workStatus){
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
    }
    public void removeLoadingPanel(int workStatus){
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        if(workStatus == CapitalizeClient.CONNECT_SUCCESS){
            //Change button text
            Button connectBtn = (Button)findViewById(R.id.connect);
            connectBtn.setText("Stop");
            //Set enable pause button
            Button pauseBtn = (Button)findViewById(R.id.pause);
            pauseBtn.setEnabled(true);
            isRunning = true;

            CharSequence text = "Connection success.";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }else if(workStatus == CapitalizeClient.CONNECT_FAIL){

            CharSequence text = "Fail to connect.";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();

        }else if(workStatus == CapitalizeClient.DISCONNECT_SUCCESS){
            //Change button text
            Button connectBtn = (Button)findViewById(R.id.connect);
            connectBtn.setText("Start");
            //Set enable pause button
            Button pauseBtn = (Button)findViewById(R.id.pause);
            pauseBtn.setEnabled(false);
            isRunning = false;

            CharSequence text = "Disconnected";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }else if(workStatus == CapitalizeClient.DISCONNECT_FAIL){

        }

    }

    public void goConnect(View view){
        if(!isRunning) {
            EditText address = (EditText)findViewById(R.id.ipAddress);
            String sAddress = address.getText().toString();

            playTask = new ClientTask();
            playTask.execute(sAddress, "start");
        }
        else{
            playTask = new ClientTask();
            playTask.execute(null, "stop");
        }
    }

    public void goPause(View view){
        if(!isRunning) {
            isRunning = true;

            playTask = new ClientTask();
            playTask.execute(null, "pause");

            Button pauseBtn = (Button)findViewById(R.id.pause);
            pauseBtn.setText("Resume");

        }
        else{
            playTask = new ClientTask();
            playTask.execute(null, "resume");

            isRunning = false;
            Button pauseBtn = (Button) findViewById(R.id.pause);
            pauseBtn.setText("Pause");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        EditText ipAddressEditText = (EditText)findViewById(R.id.ipAddress);
        ipAddressEditText.setText("169.234.220.221");

        Button pauseBtn = (Button)findViewById(R.id.pause);
        pauseBtn.setEnabled(false);

        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
