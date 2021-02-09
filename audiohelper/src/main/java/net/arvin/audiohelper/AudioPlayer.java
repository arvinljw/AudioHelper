package net.arvin.audiohelper;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;

/**
 * Created by arvinljw on 2019-11-14 17:45
 * Function：
 * Desc：音频播放器
 */
public class AudioPlayer implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private PlayStateCallback playStateCallback;
    private String url;
    private boolean isPlaying;

    public AudioPlayer(Context context, PlayStateCallback playStateCallback) {
        this.playStateCallback = playStateCallback;
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);// 设置媒体流类型
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPlayStateCallback(PlayStateCallback playStateCallback) {
        this.playStateCallback = playStateCallback;
    }

    /**
     * @param url url地址
     */
    public void play(String url) {
        try {
            this.url = url;
            isPlaying = true;
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(url); // 设置数据源
            mMediaPlayer.prepareAsync(); // prepare自动播放
            playStateCallback.onStarted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play() {
        isPlaying = true;
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    // 暂停
    public void pause() {
        isPlaying = false;
        if (mMediaPlayer != null)
            mMediaPlayer.pause();
    }

    // 停止
    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            isPlaying = false;
        }
    }

    //释放
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            url = null;
        }
    }

    public void seekTo(int time) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(time);
            mMediaPlayer.start();
        }
    }

    //准备播放
    @Override
    public void onPrepared(MediaPlayer mp) {
        if (playStateCallback != null) {
            playStateCallback.onPrepared();
        }
        mp.start();
    }

    //播放完成
    @Override
    public void onCompletion(MediaPlayer mp) {
        stop();

        if (playStateCallback != null) {
            playStateCallback.onCompletion();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public long getDuration() {
        return mMediaPlayer.getDuration();
    }

    /**
     * 获取当前播放时长
     *
     * @return 本地播放时长
     */
    public static long getDurationLocation(Context context, String path) {
        MediaPlayer player = MediaPlayer.create(context, Uri.fromFile(new File(path)));
        if (player != null) {
            return player.getDuration();
        } else {
            return 0;
        }
    }

    public boolean isSame(String url) {
        if (this.url == null) {
            return false;
        }
        return this.url.equals(url);
    }

    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return isPlaying;
        }
        return false;
    }

    public interface PlayStateCallback {
        void onStarted();

        void onPrepared();

        void onCompletion();
    }
}
