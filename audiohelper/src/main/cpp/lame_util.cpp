#include <jni.h>
#include <string>

extern "C" {
#include "lame.h"
}

static lame_global_flags *lame = nullptr;

extern "C"
JNIEXPORT void JNICALL
Java_net_arvin_audiohelper_AudioRecorder_init(JNIEnv *env, jclass clazz, jint in_samplerate,
                                              jint in_channel, jint out_samplerate,
                                              jint out_bitrate, jint quality) {
    if (lame != nullptr) {
        lame_close(lame);
        lame = nullptr;
    }

    lame = lame_init();

    lame_set_in_samplerate(lame, in_samplerate);
    lame_set_num_channels(lame, in_channel);
    lame_set_out_samplerate(lame, out_samplerate);
    lame_set_brate(lame, out_bitrate);
    lame_set_quality(lame, quality);
    lame_init_params(lame);
}

extern "C"
JNIEXPORT jint JNICALL
Java_net_arvin_audiohelper_AudioRecorder_encode(JNIEnv *env, jclass clazz, jshortArray buffer_left,
                                                jshortArray buffer_right, jint samples,
                                                jbyteArray mp3buf) {
    jshort *j_buffer_l = env->GetShortArrayElements(buffer_left, nullptr);

    jshort *j_buffer_r = env->GetShortArrayElements(buffer_right, nullptr);

    const jsize mp3buf_size = env->GetArrayLength(mp3buf);
    jbyte *j_mp3buf = env->GetByteArrayElements(mp3buf, nullptr);

    int result = lame_encode_buffer(lame, j_buffer_l, j_buffer_r,
                                    samples, reinterpret_cast<unsigned char *>(j_mp3buf),
                                    mp3buf_size);

    env->ReleaseShortArrayElements(buffer_left, j_buffer_l, 0);
    env->ReleaseShortArrayElements(buffer_right, j_buffer_r, 0);
    env->ReleaseByteArrayElements(mp3buf, j_mp3buf, 0);

    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_net_arvin_audiohelper_AudioRecorder_flush(JNIEnv *env, jclass clazz, jbyteArray mp3buf) {
    const jsize mp3buf_size = env->GetArrayLength(mp3buf);
    jbyte *j_mp3buf = env->GetByteArrayElements(mp3buf, nullptr);

    int result = lame_encode_flush(lame, reinterpret_cast<unsigned char *>(j_mp3buf), mp3buf_size);

    env->ReleaseByteArrayElements(mp3buf, j_mp3buf, 0);

    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_net_arvin_audiohelper_AudioRecorder_close(JNIEnv *env, jclass clazz) {
    lame_close(lame);
    lame = nullptr;
}
