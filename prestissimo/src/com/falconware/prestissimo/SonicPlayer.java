//Copyright 2012 James Falcon
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package com.falconware.prestissimo;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.*;
import android.net.Uri;
import android.os.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vinuxproject.sonic.Sonic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class SonicPlayer {
    private static final Logger log = LoggerFactory.getLogger("SonicPlayer");
    private AudioTrack mTrack;
    private Sonic mSonic;
    private MediaExtractor mExtractor;
    private MediaCodec mCodec;
    private Thread mDecoderThread;
    private String mPath;
    private Uri mUri;
    private final ReentrantLock mLock;
    private final Object mDecoderLock;
    private boolean mContinue;
    private boolean mIsDecoding;
    private long mDuration;
    private float mCurrentSpeed;
    private float mCurrentPitch;
    private int mCurrentState;
    private final Context mContext;
    private float mVolumeGain = 1f;

    private final static int STATE_IDLE = 0;
    private final static int STATE_INITIALIZED = 1;
    private final static int STATE_PREPARING = 2;
    private final static int STATE_PREPARED = 3;
    private final static int STATE_STARTED = 4;
    private final static int STATE_PAUSED = 5;
    private final static int STATE_STOPPED = 6;
    private final static int STATE_PLAYBACK_COMPLETED = 7;
    private final static int STATE_END = 8;
    private final static int STATE_ERROR = 9;

    // Not available in API 16 :(
    private final static int MEDIA_ERROR_MALFORMED = 0xfffffc11;
    private final static int MEDIA_ERROR_IO = 0xfffffc14;

    private final static long SEEK_TOLERANCE = 1000000;

    // The aidl interface should automatically implement stubs for these, so
    // don't initialize or require null checks.
    protected OnErrorListener errorCallback;
    protected OnCompletionListener completionCallback;
    protected OnBufferingUpdateListener bufferingUpdateCallback;
    protected OnInfoListener infoCallback;
    protected OnPreparedListener preparedCallback;
    protected OnSeekCompleteListener seekCompleteCallback;

    public SonicPlayer(Context context) {
        mCurrentState = STATE_IDLE;
        mCurrentSpeed = (float) 1.0;
        mCurrentPitch = (float) 1.0;
        mContinue = false;
        mIsDecoding = false;
        mContext = context;
        mPath = null;
        mUri = null;
        mLock = new ReentrantLock();
        mDecoderLock = new Object();
    }

    public void setOnErrorListener(OnErrorListener l) {
        errorCallback = l;
    }

    public void setOnCompletionListener(OnCompletionListener l) {
        completionCallback = l;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener l) {
        bufferingUpdateCallback = l;
    }

    public void setOnInfoListener(OnInfoListener l) {
        infoCallback = l;
    }

    public void setOnPreparedListener(OnPreparedListener l) {
        preparedCallback = l;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener l) {
        seekCompleteCallback = l;
    }

    private boolean fireErrorListener(int what, int extra) {
        return errorCallback != null && errorCallback.onError(what, extra);
    }

    private void fireCompletionListener() {
        if (completionCallback != null) {
            completionCallback.onCompletion();
        }
    }

    private void fireBufferingUpdateListener(int percent) {
        if (bufferingUpdateCallback != null) {
            bufferingUpdateCallback.onBufferingUpdate(percent);
        }
    }

    private void fireInfoListener(int what, int extra) {
        if (infoCallback != null) {
            infoCallback.onInfo(what, extra);
        }
    }

    private void firePreparedListener() {
        if (preparedCallback != null) {
            preparedCallback.onPrepared();
        }
    }

    private void fireSeekCompleteListener() {
        if (seekCompleteCallback != null) {
            seekCompleteCallback.onSeekComplete();
        }
    }

    // TODO: This probably isn't right...
    public float getCurrentPitchStepsAdjustment() {
        return mCurrentPitch;
    }

    public int getCurrentPosition() {
        switch (mCurrentState) {
        case STATE_ERROR:
            error();
            break;
        default:
            return (int) (mExtractor.getSampleTime() / 1000);
        }
        return 0;
    }

    public float getCurrentSpeed() {
        return mCurrentSpeed;
    }

    public int getDuration() {
        switch (mCurrentState) {
        case STATE_INITIALIZED:
        case STATE_IDLE:
        case STATE_ERROR:
            error();
            break;
        default:
            return (int) (mDuration / 1000);
        }
        return 0;
    }

    public boolean isPlaying() {
        switch (mCurrentState) {
        case STATE_ERROR:
            error();
            break;
        default:
            return mCurrentState == STATE_STARTED;
        }
        return false;
    }

    public void pause() {
        switch (mCurrentState) {
        case STATE_STARTED:
        case STATE_PAUSED:
            mTrack.pause();
            mCurrentState = STATE_PAUSED;
            log.info("State changed to STATE_PAUSED");
            break;
        default:
            error();
        }
    }

    public void prepare() {
        switch (mCurrentState) {
        case STATE_INITIALIZED:
        case STATE_STOPPED:
            try {
                initStream();
            } catch (IOException e) {
                log.error("Failed setting data source!", e);
                error();
                return;
            }
            mCurrentState = STATE_PREPARED;
            log.info("State changed to STATE_PREPARED");
            firePreparedListener();
            break;
        default:
            error();
        }
    }

    public void prepareAsync() {
        switch (mCurrentState) {
        case STATE_INITIALIZED:
        case STATE_STOPPED:
            mCurrentState = STATE_PREPARING;
            log.info("State changed to STATE_PREPARING");

            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        initStream();
                    } catch (IOException e) {
                        log.error("Failed setting data source!", e);
                        error();
                        return;
                    } catch (NullPointerException e) {
                        log.error("Failed setting data source (possibly cannot access stream properties)!", e);
                        error();
                        return;
                    } catch (Throwable e) {
                        log.error("Failed setting data source", e);
                        error();
                        return;
                    }
                    if (mCurrentState != STATE_ERROR) {
                        mCurrentState = STATE_PREPARED;
                        log.info("State changed to STATE_PREPARED");
                    }
                    firePreparedListener();

                }
            });
            t.setDaemon(true);
            t.start();

            break;
        default:
            error();
        }
    }

    public void stop() {
        switch (mCurrentState) {
        case STATE_PREPARED:
        case STATE_STARTED:
        case STATE_STOPPED:
        case STATE_PAUSED:
        case STATE_PLAYBACK_COMPLETED:
            mCurrentState = STATE_STOPPED;
            log.info("State changed to STATE_STOPPED");
            mContinue = false;
            mTrack.pause();
            mTrack.flush();
            break;
        default:
            error();
        }
    }

    public void start() {
        switch (mCurrentState) {
        case STATE_PREPARED:
        case STATE_PLAYBACK_COMPLETED:
            mCurrentState = STATE_STARTED;
            log.info("State changed to STATE_STARTED");
            mContinue = true;
            mTrack.play();
            decode();
        case STATE_STARTED:
            break;
        case STATE_PAUSED:
            mCurrentState = STATE_STARTED;
            log.info("State changed to STATE_STARTED");
            synchronized (mDecoderLock) {
                mDecoderLock.notify();
            }
            mTrack.play();
            break;
        default:
            mCurrentState = STATE_ERROR;
            log.info("State changed to STATE_ERROR in start");
            if (mTrack != null) {
                error();
            } else {
                log.error("start",
                        "Attempting to start while in idle after construction.  Not allowed by no callbacks called");
            }
        }
    }

    public void release() {
        reset();
        errorCallback = null;
        completionCallback = null;
        bufferingUpdateCallback = null;
        infoCallback = null;
        preparedCallback = null;
        seekCompleteCallback = null;
        mCurrentState = STATE_END;
    }

    public void reset() {
        mLock.lock();
        try {
            mContinue = false;
            try {
                if (mDecoderThread != null
                        && mCurrentState != STATE_PLAYBACK_COMPLETED) {
                    while (mIsDecoding) {
                        synchronized (mDecoderLock) {
                            mDecoderLock.notify();
                            mDecoderLock.wait();
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.error(
                        "Interrupted in reset while waiting for decoder thread to stop.",
                        e);
            }
            if (mCodec != null) {
                mCodec.release();
                mCodec = null;
            }
            if (mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }
            if (mTrack != null) {
                mTrack.release();
                mTrack = null;
            }
            mCurrentState = STATE_IDLE;
            log.info("State changed to STATE_IDLE");
        } finally {
            mLock.unlock();
        }
    }

    public void seekTo(final int msec) {
        switch (mCurrentState) {
        case STATE_PREPARED:
        case STATE_STARTED:
        case STATE_PAUSED:
        case STATE_PLAYBACK_COMPLETED:
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mLock.lock();
                        if (mTrack == null) {
                            return;
                        }
                        mTrack.flush();
                        final long micros = (long) msec * 1000;
                        log.debug("starting to seek to " + micros);
                        mExtractor.seekTo(micros, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                        log.debug("seeked to " + mExtractor.getSampleTime() / 1000);
                        while ((mExtractor.getSampleTime() < micros - SEEK_TOLERANCE) && mExtractor.advance()) ;
                        log.debug("advanced seek to " + mExtractor.getSampleTime() / 1000);
                        fireSeekCompleteListener();
                    } finally {
                        mLock.unlock();
                    }
                }
            });
            t.setDaemon(true);
            t.start();
            break;
        default:
            error();
        }
    }

    public void setDataSource(String path) {
        switch (mCurrentState) {
        case STATE_IDLE:
            mPath = path;
            mCurrentState = STATE_INITIALIZED;
            log.info("Moving state to STATE_INITIALIZED");
            break;
        default:
            error();
        }
    }

    public void setDataSource(Uri uri) {
        switch (mCurrentState) {
        case STATE_IDLE:
            mUri = uri;
            mCurrentState = STATE_INITIALIZED;
            log.info("Moving state to STATE_INITIALIZED");
            break;
        default:
            error();
        }
    }

    public void setVolume(float volume) {
        mTrack.setStereoVolume(volume, volume);
    }

    public void setPlaybackPitch(float f) {
        mCurrentPitch = f;
    }

    public void setPlaybackSpeed(float f) {
        mCurrentSpeed = f;
    }

    /**
     * The current volume gain in dB
     */
    public float getVolumeGain() {
        return mVolumeGain;
    }

    /**
     * Sets the volume gain in dB
     */
    public void setVolumeGain(float gain) {
        mVolumeGain = gain;
    }

    private float getVolumeMultiplier() {
        return (float) Math.pow(10, mVolumeGain / 10);
    }

    public void error() {
        error(0);
    }

    public void error(int extra) {
        log.error("Moved to error state!");
        mCurrentState = STATE_ERROR;
        boolean handled = fireErrorListener(
                MediaPlayer.MEDIA_ERROR_UNKNOWN, extra);
        if (!handled) {
            fireCompletionListener();
        }
    }

    private int findFormatFromChannels(int numChannels) {
        switch (numChannels) {
        case 1:
            return AudioFormat.CHANNEL_OUT_MONO;
        case 2:
            return AudioFormat.CHANNEL_OUT_STEREO;
        default:
            return -1; // Error
        }
    }

    private int findFirstAudioTrackIndex(MediaExtractor extractor) {
        for (int trackIndex = 0; trackIndex < extractor.getTrackCount(); ++trackIndex) {
            final MediaFormat format = extractor.getTrackFormat(trackIndex);
            final String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.toLowerCase().startsWith("audio"))
                return trackIndex;
        }
        return 0;
    }

    public void initStream() throws IOException {
        mLock.lock();
        try {
            mExtractor = new MediaExtractor();
            if (mPath != null) {
                mExtractor.setDataSource(mPath);
            } else if (mUri != null) {
                mExtractor.setDataSource(mContext, mUri, null);
            } else {
                throw new IOException();
            }

            final int audioTrackIndex = findFirstAudioTrackIndex(mExtractor);
            final MediaFormat oFormat = mExtractor.getTrackFormat(audioTrackIndex);
            int sampleRate = oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            final String mime = oFormat.getString(MediaFormat.KEY_MIME);
            mDuration = oFormat.getLong(MediaFormat.KEY_DURATION);

            log.info("Sample rate: " + sampleRate);
            log.info("Mime type: " + mime);

            initDevice(sampleRate, channelCount);
            mExtractor.selectTrack(audioTrackIndex);
            mCodec = MediaCodec.createDecoderByType(mime);
            mCodec.configure(oFormat, null, null, 0);
        } finally {
            mLock.unlock();
        }
    }

    private void initDevice(int sampleRate, int numChannels) {
        mLock.lock();
        try {
            final int format = findFormatFromChannels(numChannels);
            final int minSize = AudioTrack.getMinBufferSize(sampleRate, format,
                    AudioFormat.ENCODING_PCM_16BIT);
            mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, format,
                    AudioFormat.ENCODING_PCM_16BIT, minSize * 4,
                    AudioTrack.MODE_STREAM);
            mSonic = new Sonic(sampleRate, numChannels);
        } finally {
            mLock.unlock();
        }
    }

    public void decode() {
        mDecoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mIsDecoding = true;
                mCodec.start();

                ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
                ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();

                boolean sawInputEOS = false;
                boolean sawOutputEOS = false;

                while (!sawInputEOS && !sawOutputEOS && mContinue) {
                    if (mCurrentState == STATE_PAUSED) {
                        System.out.println("Decoder changed to PAUSED");
                        try {
                            synchronized (mDecoderLock) {
                                mDecoderLock.wait();
                                System.out.println("Done with wait");
                            }
                        } catch (InterruptedException e) {
                            // Purposely not doing anything here
                        }
                        continue;
                    }

                    if (null != mSonic) {
                        mSonic.setSpeed(mCurrentSpeed);
                        mSonic.setPitch(mCurrentPitch);
                        mSonic.setVolume(getVolumeMultiplier());
                    }

                    int inputBufIndex = mCodec.dequeueInputBuffer(200);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = inputBuffers[inputBufIndex];
                        int sampleSize = mExtractor.readSampleData(dstBuf, 0);
                        long presentationTimeUs = 0;
                        if (sampleSize < 0) {
                            sawInputEOS = true;
                            sampleSize = 0;
                        } else {
                            presentationTimeUs = mExtractor.getSampleTime();
                        }
                        mCodec.queueInputBuffer(
                                inputBufIndex,
                                0,
                                sampleSize,
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                        : 0);
                        if (!sawInputEOS) {
                            mExtractor.advance();
                        }
                    }

                    final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    byte[] modifiedSamples = new byte[info.size];

                    int res;
                    do {
                        res = mCodec.dequeueOutputBuffer(info, 200);
                        if (res >= 0) {
                            int outputBufIndex = res;
                            ByteBuffer buf = outputBuffers[outputBufIndex];
                            final byte[] chunk = new byte[info.size];
                            outputBuffers[res].get(chunk);
                            outputBuffers[res].clear();

                            if (chunk.length > 0) {
                                mSonic.putBytes(chunk, chunk.length);
                            } else {
                                mSonic.flush();
                            }
                            int available = mSonic.availableBytes();
                            if (available > 0) {
                                if (modifiedSamples.length < available) {
                                    modifiedSamples = new byte[available];
                                }
                                mSonic.receiveBytes(modifiedSamples, available);
                                mTrack.write(modifiedSamples, 0, available);
                            }

                            mCodec.releaseOutputBuffer(outputBufIndex, false);

                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                sawOutputEOS = true;
                            }
                        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            outputBuffers = mCodec.getOutputBuffers();
                            log.debug("PCM: Output buffers changed");
                        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            mTrack.stop();
                            mLock.lock();
                            try {
                                mTrack.release();
                                final MediaFormat oformat = mCodec
                                        .getOutputFormat();
                                log.debug("PCM: Output format has changed to"
                                        + oformat);
                                initDevice(
                                        oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                        oformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                                outputBuffers = mCodec.getOutputBuffers();
                                mTrack.play();
                            } finally {
                                mLock.unlock();
                            }
                        }
                    } while (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
                            || res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED);
                }
                log.debug(
                        "Decoding loop exited. Stopping codec and track");
                log.debug("Duration: " + (int) (mDuration / 1000));
                log.debug(
                        "Current position: "
                                + (int) (mExtractor.getSampleTime() / 1000));
                mCodec.stop();
                mTrack.stop();
                log.debug("Stopped codec and track");
                log.debug(
                        "Current position: "
                                + (int) (mExtractor.getSampleTime() / 1000));
                mIsDecoding = false;
                if (mContinue && (sawInputEOS || sawOutputEOS)) {
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            fireCompletionListener();
                        }
                    });
                    t.setDaemon(true);
                    t.start();
                } else {
                    log.debug(
                            "Loop ended before saw input eos or output eos");
                    log.debug("sawInputEOS: " + sawInputEOS);
                    log.debug("sawOutputEOS: " + sawOutputEOS);
                }
                synchronized (mDecoderLock) {
                    mDecoderLock.notifyAll();
                }
            }
        });
        mDecoderThread.setDaemon(true);
        mDecoderThread.start();
    }

    //
    // interfaces
    //

    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(int percent);
    }

    public interface OnCompletionListener {
        void onCompletion();
    }

    public interface OnErrorListener {
        boolean onError(int what, int extra);
    }

    public interface OnInfoListener {
        boolean onInfo(int what, int extra);
    }

    public interface OnPitchAdjustmentAvailableChangedListener {
        void onPitchAdjustmentAvailableChanged(boolean pitchAdjustmentAvailable);
    }

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnSeekCompleteListener {
        void onSeekComplete();
    }

    public interface OnSpeedAdjustmentAvailableChangedListener {
        void onSpeedAdjustmentAvailableChanged(boolean speedAdjustmentAvailable);
    }
}
