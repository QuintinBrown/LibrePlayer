package com.QuintinBrown.LibrePlayer.playback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import javax.sound.sampled.AudioFormat;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVFrameSideData;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;

public class Decoder implements AutoCloseable {

    private Path source;

    private AVFormatContext formatContext;
    private AVCodecContext codecContext;
    private AVCodec codec;
    private AVStream audioStream;

    private AVChannelLayout channelLayout;
    private int sampleFormat;

    private AVPacket packet;
    private AVFrame frame;

    private SwrContext swrContext;

    private int audioStreamIndex;

    private int pcmBufferSize;

    private int sampleRate;
    private int channels;
    private AudioFormat outputFormat;

    private int outputSampleRate;
    private int outputChannels;
    private int outputSampleFormat;
    private static final int MAX_OUTPUT_SAMPLES = 8192;

    private boolean eof;
    private boolean initialized;

    private byte[] pcmCache;
    private int pcmCachePosition;
    private int pcmCacheLength;

    private long currentPts = avutil.AV_NOPTS_VALUE;

    private long pendingSkipSamples = 0;
    private int lastTrimSkipSamples = 0;
    private int lastTrimDiscardSamples = 0;
    private long pendingFrameDiscard = 0;

    private BytePointer pcmBuffer;

    private String getErrorString(int errorCode) {
        BytePointer buffer = new BytePointer(1024);
        
        avutil.av_strerror(errorCode, buffer, 1024);

        return buffer.getString();
    }

    private void checkError(int error) {
        if (error < 0) {
            throw new RuntimeException(getErrorString(error));
        }
    }

    private void initializeFormatContext() {
        this.formatContext = avformat.avformat_alloc_context();

        if (formatContext == null) {
            throw new RuntimeException("Failed allocating AVFormatContext");
        }

        int error = avformat.avformat_open_input(
            formatContext,
            source.toAbsolutePath().toString(),
            null,
            null
        );

        checkError(error);

        error = avformat.avformat_find_stream_info(formatContext, (PointerPointer<?>) null);

        checkError(error);

        this.audioStreamIndex = avformat.av_find_best_stream(formatContext, avutil.AVMEDIA_TYPE_AUDIO, -1, -1, (PointerPointer<?>) null, 0);
        
        if (audioStreamIndex < 0) {
            throw new RuntimeException("No playable audio stream");
        }

        this.audioStream = formatContext.streams(audioStreamIndex);

    }

    private void initializeCodec() {
        AVCodecParameters codecParameters = audioStream.codecpar();

        this.codec = avcodec.avcodec_find_decoder(codecParameters.codec_id());

        if (codec == null) {
            throw new RuntimeException("Unsupported codec: " + codecParameters.codec_id());
        }

        this.codecContext = avcodec.avcodec_alloc_context3(codec);

        if (codecContext == null) {
            throw new RuntimeException("Failed to allocate AVCodecContext");
        }

        checkError(avcodec.avcodec_parameters_to_context(codecContext, codecParameters));

        checkError(avcodec.avcodec_open2(codecContext, codec, (AVDictionary) null));

        this.sampleRate = codecContext.sample_rate();
        this.channels = codecContext.ch_layout().nb_channels();
        this.channelLayout = new AVChannelLayout();
        avutil.av_channel_layout_copy(channelLayout, codecContext.ch_layout());
        this.sampleFormat = codecContext.sample_fmt();
    }

    private void initializeResampler() {
        AVChannelLayout outputLayout = new AVChannelLayout();

        checkError(avutil.av_channel_layout_copy(outputLayout, codecContext.ch_layout()));

        this.swrContext = swresample.swr_alloc();

        if (swrContext == null) {
            throw new RuntimeException("Failed to allocate SwrContext");
        }

        checkError(
            swresample.swr_alloc_set_opts2(
                swrContext,

                // Output
                outputLayout,
                avutil.AV_SAMPLE_FMT_S16,
                codecContext.sample_rate(),

                // Input
                codecContext.ch_layout(),
                codecContext.sample_fmt(),
                codecContext.sample_rate(),

                0,
                null
            )
        );

        checkError(swresample.swr_init(swrContext));

        this.outputSampleFormat = avutil.AV_SAMPLE_FMT_S16;
        this.outputSampleRate = codecContext.sample_rate();
        this.outputChannels = outputLayout.nb_channels();

        this.outputFormat = new AudioFormat(outputSampleRate, 16, outputChannels, true, false);

        outputLayout.close();
    }

    private boolean decodeNextFrame() {
        while (true) {

            avutil.av_frame_unref(frame);

            // try to pull queued frame
            int result = avcodec.avcodec_receive_frame(codecContext, frame);

            if (result == 0) {
                currentPts = frame.best_effort_timestamp();
                return true;
            }

            if (result == avutil.AVERROR_EOF) {
                eof = true;
                return false;
            }

            if (result != avutil.AVERROR_EAGAIN()) {
                checkError(result);
            }

            while (true) {
                if (packet.size() == 0) {
                    int readResult = avformat.av_read_frame(formatContext, packet);

                    if (readResult == avutil.AVERROR_EOF) {
                        checkError(avcodec.avcodec_send_packet(codecContext, null));
                        break;
                    }

                    checkError(readResult);

                    if (packet.stream_index() != audioStreamIndex) {
                        avcodec.av_packet_unref(packet);
                        continue;
                    }
                }

                int sendResult = avcodec.avcodec_send_packet(codecContext, packet);

                if (sendResult == 0) {
                    avcodec.av_packet_unref(packet);
                    break;
                }

                if (sendResult == avutil.AVERROR_EAGAIN()) {
                    break;
                }

                avcodec.av_packet_unref(packet);
                checkError(sendResult);
            }
        }
    }

    private int convertFrametoPCM(BytePointer output) {
        checkGaplessSideData();

        int maxPossibleSamples = swresample.swr_get_out_samples(swrContext, frame.nb_samples());

        if (maxPossibleSamples < 0) {
            throw new RuntimeException("Failed to compute output sample count: " + getErrorString(maxPossibleSamples));
        }

        if (maxPossibleSamples > MAX_OUTPUT_SAMPLES) {
            throw new RuntimeException(
                "PCM buffer too small, need capacity for: " + maxPossibleSamples + "only have: " + MAX_OUTPUT_SAMPLES
            );
        }

        PointerPointer<BytePointer> outputPointer = new PointerPointer<>(1);
        outputPointer.put(0, output);

        int outputSamples = swresample.swr_convert(
            swrContext,
            outputPointer,
            MAX_OUTPUT_SAMPLES,
            frame.data(),
            frame.nb_samples()
        );

        if (outputSamples < 0) {
            outputPointer.close();
            throw new RuntimeException("Failed to resample: " + getErrorString(outputSamples));
        }

        int skip = (int) Math.min(pendingSkipSamples, outputSamples);
        pendingSkipSamples -= skip;

        int remaining = outputSamples - skip;
        int discard = (int) Math.min(pendingFrameDiscard, remaining);

        lastTrimSkipSamples = skip;
        lastTrimDiscardSamples = discard;

        int bytesPerSample = avutil.av_get_bytes_per_sample(avutil.AV_SAMPLE_FMT_S16);

        outputPointer.close();

        return outputSamples * outputChannels * bytesPerSample;
    }

    private void flushDecoder() {
        if (codecContext != null) {
            avcodec.avcodec_flush_buffers(codecContext);
        }

        if (swrContext != null) {
            swresample.swr_close(swrContext);
            checkError(swresample.swr_init(swrContext));
        }

        this.eof = false;
    }

    private void clean() {
        if (frame != null) {
            avutil.av_frame_unref(frame);
            avutil.av_frame_free(frame);
            frame = null;
        }

        if (packet != null) {
            avcodec.av_packet_unref(packet);
            avcodec.av_packet_free(packet);
            packet = null;
        }

        if (swrContext != null) {
            swresample.swr_free(swrContext);
            swrContext = null;
        }

        if (codecContext != null) {
            avcodec.avcodec_free_context(codecContext);
            codecContext = null;
        }

        if (formatContext != null) {
            avformat.avformat_close_input(formatContext);
            formatContext = null;
        }

        if (pcmBuffer != null) {
            pcmBuffer.close();
            pcmBuffer = null;
        }

        if (channelLayout != null) {
            avutil.av_channel_layout_uninit(channelLayout);
            channelLayout.close();
            channelLayout = null;
        }

        audioStream = null;
        codec = null;

        initialized = false;
        eof = false;
    }

    private void checkGaplessSideData() {
        AVFrameSideData sd = avutil.av_frame_get_side_data(frame, avutil.AV_FRAME_DATA_SKIP_SAMPLES);

        long frameDiscard = 0;

        if (sd != null && !sd.isNull()) {
            BytePointer data = sd.data();
            long skip = data.getInt(0) & 0xFFFFFFFFL;
            frameDiscard = data.getInt(4) & 0xFFFFFFFFL;

            if (skip > 0) {
                pendingSkipSamples += skip;
            }
        }

        this.pendingFrameDiscard = frameDiscard;
    }
    
    public void open(Path file) {
        if (initialized) {
            close();
        }

        Objects.requireNonNull(file, "file");

        if (!Files.exists(file)) {
            throw new RuntimeException("File does not exist: " + file);
        }

        this.source = file;

        try {
            initializeFormatContext();
            initializeCodec();
            initializeResampler();

            int bytesPerSample = avutil.av_get_bytes_per_sample(outputSampleFormat);

            pcmBufferSize = MAX_OUTPUT_SAMPLES * outputChannels * bytesPerSample;

            pcmBuffer = new BytePointer(pcmBufferSize);

            if (pcmBuffer == null) {
                throw new RuntimeException("Failed to allocate PCM buffer");
            }

            pcmCache = new byte[pcmBufferSize];
            pcmCachePosition = 0;
            pcmCacheLength = 0;

            packet = avcodec.av_packet_alloc();

            if (packet == null) {
                throw new RuntimeException("Failed to allocate AVPacket");
            }

            frame = avutil.av_frame_alloc();

            if (frame == null) {
                throw new RuntimeException("Failed to allocate AVFrame");
            }

            eof = false;
            initialized = true;

        } catch (Exception e) {
            clean();
            throw e;
        }
    }

    public int read(byte[] buffer) {
        int totalCopied = 0;

        while (totalCopied < buffer.length) {
            if (pcmCachePosition >= pcmCacheLength) {
                // EOF
                if (!decodeNextFrame()) {
                    if (totalCopied == 0) {
                        return -1;
                    }

                    break;
                }

                int bytes = convertFrametoPCM(pcmBuffer);
                pcmBuffer.position(0);
                pcmBuffer.get(pcmCache, 0, bytes);

                int bytesPerSample = avutil.av_get_bytes_per_sample(avutil.AV_SAMPLE_FMT_S16);
                int frameStride = outputChannels * bytesPerSample;

                pcmCachePosition = lastTrimSkipSamples * frameStride;
                pcmCacheLength = bytes - (lastTrimDiscardSamples * frameStride);
            }

            int available = pcmCacheLength - pcmCachePosition;

            int wanted = buffer.length - totalCopied;

            int copy = Math.min(available, wanted);

            System.arraycopy(pcmCache, pcmCachePosition, buffer, totalCopied, copy);

            pcmCachePosition += copy;
            totalCopied += copy;
        }

        return totalCopied;
    }

    public long seek(Duration position, int flags) {
        Objects.requireNonNull(position, "position");

        long timestamp = avutil.av_rescale_q(position.toMillis(), avutil.av_make_q(1, 1000), audioStream.time_base());

        checkError(
            avformat.av_seek_frame(formatContext, audioStreamIndex, timestamp, flags)
        );

        flushDecoder();

        avcodec.av_packet_unref(packet);
        avutil.av_frame_unref(frame);

        pcmCachePosition = 0;
        pcmCacheLength = 0;

        return timestamp;
    }

    public AudioFormat getOutputFormat(){return outputFormat;}

    public boolean isFinished() {
        return eof && pcmCachePosition >= pcmCacheLength;
    }

    public int getOutputSampleRate(){return outputSampleRate;}

    public int getOutputChannels(){return outputChannels;}

    public long getTotalSamples(){
        if (audioStream == null) {
            return 0;
        }

        AVRational timeBase = audioStream.time_base();

        long duration = audioStream.duration();

        if (duration == avutil.AV_NOPTS_VALUE) {
            return 0;
        }

        double seconds = duration * avutil.av_q2d(timeBase);

        return (long) (seconds * outputSampleRate);
    }

    public Duration getDuration(){
        if (audioStream == null) {
            return Duration.ZERO;
        }

        long duration = audioStream.duration();

        if (duration == avutil.AV_NOPTS_VALUE) {
            return Duration.ZERO;
        }

        double seconds = duration * avutil.av_q2d(audioStream.time_base());

        return Duration.ofMillis((long) (seconds * 1000));
    }

    public Duration getCurrentTime(){
        if (currentPts == avutil.AV_NOPTS_VALUE || audioStream == null) {
            return Duration.ZERO;
        }

        double seconds = currentPts * avutil.av_q2d(audioStream.time_base());

        return Duration.ofMillis((long) (seconds * 1000));
    }

    public String getCodecName(){if (codec == null){return null;} return codec.name().getString();}

    public long getBitrate(){if (codecContext == null){return 0;} return codecContext.bit_rate();}

    public Path getSource() {
        return source;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isEOF() {
        return eof;
    }

    public int getAudioStreamIndex() {
        return audioStreamIndex;
    }

    public String getSampleFormatName() {
        return avutil.av_get_sample_fmt_name(sampleFormat).getString();
    }

    public int getBitDepth() {
        return avutil.av_get_bytes_per_sample(outputSampleFormat) * 8;
    }

    @Override
    public void close() {
        clean();
    }
}
