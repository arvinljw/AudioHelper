package net.arvin.audiohelper.demo;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import net.arvin.audiohelper.AudioPlayer;
import net.arvin.audiohelper.AudioRecorder;
import net.arvin.permissionhelper.PermissionUtil;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AudioRecorder.VolumeCallback, AudioPlayer.PlayStateCallback {
    private ProgressBar recordProgress;
    private ProgressBar playProgress;

    private AudioRecorder audioRecorder;
    private File recordFile;

    private AudioPlayer audioPlayer;
    private boolean isPause;
    private PermissionUtil permissionUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_record_start).setOnClickListener(this);
        findViewById(R.id.btn_record_stop).setOnClickListener(this);
        findViewById(R.id.btn_play_start).setOnClickListener(this);
        findViewById(R.id.btn_play_pause).setOnClickListener(this);
        findViewById(R.id.btn_play_stop).setOnClickListener(this);

        recordProgress = findViewById(R.id.record_progress_bar);
        playProgress = findViewById(R.id.play_progress_bar);

        recordProgress.setMax(60);
        playProgress.setMax(60);

        permissionUtil = new PermissionUtil.Builder().with(this).build();
        permissionUtil.request("", new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO},
                new PermissionUtil.RequestPermissionListener() {
                    @Override
                    public void callback(boolean granted, boolean isAlwaysDenied) {
                    }
                });

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_record_start) {
            recordStart();
        } else if (id == R.id.btn_record_stop) {
            recordStop();
        } else if (id == R.id.btn_play_start) {
            playStart();
        } else if (id == R.id.btn_play_pause) {
            playPause();
        } else if (id == R.id.btn_play_stop) {
            playStop();
        }
    }

    private void recordStart() {
        if (audioRecorder == null) {
            audioRecorder = new AudioRecorder(this);
            audioRecorder.setVolumeCallback(this);
        }
        audioRecorder.startRecord();
    }

    private void recordStop() {
        audioRecorder.stopRecord();
        recordFile = audioRecorder.getRecordFile();
    }

    private void playStart() {
        if (audioPlayer == null) {
            audioPlayer = new AudioPlayer(this, this);
        }
        if (audioPlayer.isPlaying()) {
            return;
        }
        if (isPause) {
            audioPlayer.play();
        } else {
            audioPlayer.play(recordFile.getAbsolutePath());
        }
        isPause = false;
    }

    private void playPause() {
        if (audioPlayer == null) {
            return;
        }
        isPause = true;
        audioPlayer.pause();
    }

    private void playStop() {
        if (audioPlayer == null) {
            return;
        }
        isPause = false;
        audioPlayer.stop();
    }

    @Override
    public void volume(int volume, int maxVolume) {
        Log.d("ljw >>>", "volume: " + volume + ";maxVolume: " + maxVolume);
    }

    @Override
    public void onStarted() {
        Log.d("ljw >>>", "onStarted: audio is started");
    }

    @Override
    public void onPrepared() {
        Log.d("ljw >>>", "onPrepared: audio is prepared");
    }

    @Override
    public void onCompletion() {
        Log.d("ljw >>>", "onCompletion: audio is completed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioPlayer != null) {
            if (audioPlayer.isPlaying()) {
                audioPlayer.stop();
            }
            audioPlayer.release();
        }
        if (audioRecorder != null) {
            audioRecorder.onDestroy();
        }
    }
}