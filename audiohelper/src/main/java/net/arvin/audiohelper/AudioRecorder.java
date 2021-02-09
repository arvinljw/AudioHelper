package net.arvin.audiohelper;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by arvinljw on 2019-11-13 17:47
 * Function：
 * Desc：mp3录音
 */
public class AudioRecorder {

    static {
        System.loadLibrary("lame_util");
    }

    public static native void init(int inSamplerate, int inChannel,
                                   int outSamplerate, int outBitrate, int quality);

    public static native int encode(short[] bufferLeft, short[] bufferRight,
                                    int samples, byte[] mp3buf);

    public static native int flush(byte[] mp3buf);

    public static native void close();

    private static final String RECORD_DIR = "/audioHelper/records/";

    /**
     * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
     */
    private static final int DEFAULT_SAMPLING_RATE = 44100;//模拟器仅支持从麦克风输入8kHz采样率
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 自定义 每160帧作为一个周期，通知一下需要进行编码
     */
    private static final int FRAME_COUNT = 160;

    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    /**
     * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
     */
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;
    /**
     * Encoded bit rate. MP3 file will be encoded with bit rate 32kbps
     */
    private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;

    private static final int MSG_VOLUME = 0x1001;

    public static final int MAX_VOLUME = 2000;

    private String recordFileDir;
    private File recordFile;

    private AudioRecord audioRecord;
    private boolean isRecording;
    private int bufferSize;
    private short[] pcmBuffer;

    private Handler handler;
    private ExecutorService executorService;
    private AudioEncoder recordEncoder;

    private VolumeCallback volumeCallback;

    public AudioRecorder(Context context) {
        recordFileDir = getDiskCachePath(context) + RECORD_DIR;

        initHandler();
        executorService = Executors.newCachedThreadPool();
    }

    @SuppressWarnings("HandlerLeak")
    private void initHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handMessage(msg);
            }
        };
    }

    public void setVolumeCallback(VolumeCallback volumeCallback) {
        this.volumeCallback = volumeCallback;
    }

    public File getRecordFile() {
        return recordFile;
    }

    public boolean isRecording() {
        return isRecording;
    }

    private static String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }

    public void startRecord() {
        initRecordFile();

        try {
            initAudioRecord();

            record();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        isRecording = false;
    }

    private void initRecordFile() {
        String recordFilePath = generateRecordFilePath();
        try {
            recordFile = new File(recordFilePath);
            if (!recordFile.getParentFile().exists()) {
                recordFile.getParentFile().mkdirs();
            }
            if (!recordFile.exists()) {
                recordFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateRecordFilePath() {
        // 这里最好再根据用户id再细分一下目录
        return recordFileDir + System.currentTimeMillis() + ".mp3";
    }

    private void initAudioRecord() {
        bufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
                DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT);

        //与DEFAULT_AUDIO_FORMAT对应
        int bytesPerFrame = 2;

        //使能被整除，方便下面的周期性通知
        int frameSize = bufferSize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            bufferSize = frameSize * bytesPerFrame;
        }

        pcmBuffer = new short[bufferSize];

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, DEFAULT_SAMPLING_RATE,
                DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT, bufferSize);

        init(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL, DEFAULT_SAMPLING_RATE,
                DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);
    }

    private void record() {
        recordEncoder = new AudioEncoder(recordFile, bufferSize);
        recordEncoder.start();

        audioRecord.setRecordPositionUpdateListener(recordEncoder, recordEncoder.getHandler());
        audioRecord.setPositionNotificationPeriod(FRAME_COUNT);

        isRecording = true;
        audioRecord.startRecording();

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                while (isRecording) {
                    int readSize = audioRecord.read(pcmBuffer, 0, bufferSize);
                    if (readSize > 0) {
                        recordEncoder.addTask(pcmBuffer, readSize);

                        if (handler != null) {
                            int volume = calculateRealVolume(pcmBuffer, readSize);
                            Message msg = handler.obtainMessage(MSG_VOLUME);
                            msg.arg1 = volume;
                            handler.sendMessage(msg);
                        }
                    }
                }

                try {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                recordEncoder.stopEncode();
            }
        });
    }

    /**
     * 此计算方法来自samsung开发范例
     *
     * @param buffer   buffer
     * @param readSize readSize
     */
    private int calculateRealVolume(short[] buffer, int readSize) {
        double sum = 0;
        for (int i = 0; i < readSize; i++) {
            // 这里没有做运算的优化，为了更加清晰的展示代码
            sum += buffer[i] * buffer[i];
        }
        if (readSize > 0) {
            double amplitude = sum / readSize;
            return (int) Math.sqrt(amplitude);
        }
        return 0;
    }

    public void handMessage(Message msg) {
        if (msg.what == MSG_VOLUME) {
            int volume = Math.min(msg.arg1, MAX_VOLUME);
            if (volumeCallback != null) {
                volumeCallback.volume(volume, MAX_VOLUME);
            }
        }
    }

    public void onDestroy() {
        stopRecord();
        if (handler != null && handler.hasMessages(MSG_VOLUME)) {
            handler.removeMessages(MSG_VOLUME);
            handler = null;
        }
    }

    public static int getPercentVolume(int volume, int maxVolume) {
        return (int) ((100f * volume) / maxVolume);
    }

    public interface VolumeCallback {
        void volume(int volume, int maxVolume);
    }

    public static class AudioEncoder extends HandlerThread implements AudioRecord.OnRecordPositionUpdateListener {
        private static final int MSG_STOP_ENCODE = 0x1002;

        private List<RecordTask> tasks;
        private FileOutputStream fileOutputStream;
        private byte[] mp3Buffer;
        private Handler handler;

        public AudioEncoder(File file, int bufferSize) {
            super("AudioEncoder");
            tasks = Collections.synchronizedList(new ArrayList<RecordTask>());
            mp3Buffer = new byte[(int) (7200 + (bufferSize * 2 * 1.25))];
            try {
                fileOutputStream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public synchronized void start() {
            super.start();
            handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    handMessage(msg);
                }
            };
        }

        @Override
        public void onMarkerReached(AudioRecord recorder) {
        }

        @Override
        public void onPeriodicNotification(AudioRecord recorder) {
            encodePcm2Mp3();
        }

        public void addTask(short[] pcmBuffer, int readSize) {
            tasks.add(new RecordTask(pcmBuffer, readSize));
        }

        private int encodePcm2Mp3() {
            if (tasks.size() <= 0) {
                return 0;
            }
            RecordTask task = tasks.remove(0);
            int encodeSize = AudioRecorder.encode(task.pcmBuffer, task.pcmBuffer, task.readSize, mp3Buffer);
            if (encodeSize > 0) {
                try {
                    fileOutputStream.write(mp3Buffer, 0, encodeSize);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return task.readSize;
        }

        private void flushAndRelease() {
            int flushSize = AudioRecorder.flush(mp3Buffer);
            if (flushSize > 0) {
                try {
                    fileOutputStream.write(mp3Buffer, 0, flushSize);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    AudioRecorder.close();
                }
            }
        }

        Handler getHandler() {
            return handler;
        }

        void stopEncode() {
            if (handler == null) {
                return;
            }
            handler.sendEmptyMessage(MSG_STOP_ENCODE);
        }

        public void handMessage(Message msg) {
            if (msg.what == MSG_STOP_ENCODE) {
                while (encodePcm2Mp3() > 0) ;

                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }
                flushAndRelease();
                getLooper().quit();
            }
        }

        static class RecordTask {
            short[] pcmBuffer;
            int readSize;

            RecordTask(short[] pcmBuffer, int readSize) {
                this.pcmBuffer = pcmBuffer;
                this.readSize = readSize;
            }
        }
    }
}
