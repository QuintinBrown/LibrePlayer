package com.QuintinBrown.LibrePlayer.playback;

import java.io.IOException;
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

    private BytePointer pcmBuffer;

    public Decoder(Path source) {
        this.source = source;

        this.outputFormat = new AudioFormat(outputSampleRate, 16, outputChannels, true, false);
    }

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
        this.channelLayout = codecContext.ch_layout();
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

        outputLayout.close();
    }

    private boolean decodeNextFrame() {
        
        // read the next packet
        while (true) {
            int result = avformat.av_read_frame(formatContext, packet);

            if (result == avutil.AVERROR_EOF) {
                checkError(avcodec.avcodec_send_packet(codecContext, null));
            } else {
                checkError(result);

                if (packet.stream_index() != audioStreamIndex) {
                    avcodec.av_packet_unref(packet);
                    continue;
                }

                checkError(avcodec.avcodec_send_packet(codecContext, packet));

                avcodec.av_packet_unref(packet);
            }

            result = avcodec.avcodec_receive_frame(codecContext, frame);

            if (result == avutil.AVERROR_EOF) {
                this.eof = true;
                return false;
            } else if (result == 0) {
                return true;
            }

            checkError(result);
        }
    }

    private int convertFrametoPCM(BytePointer output) {
        int outputSamples = swresample.swr_convert(
            swrContext,
            new PointerPointer<>(output),
            MAX_OUTPUT_SAMPLES,
            frame.data(),
            frame.nb_samples()
        );

        if (outputSamples < 0) {
            throw new RuntimeException("Failed to resample");
        }

        int bytesPerSample = avutil.av_get_bytes_per_sample(avutil.AV_SAMPLE_FMT_S16);

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

        audioStream = null;
        codec = null;

        initialized = false;
        eof = false;
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
        if (!decodeNextFrame()) {
            return -1;
        }

        int bytes = convertFrametoPCM(pcmBuffer);

        pcmBuffer.position(0);
        pcmBuffer.get(buffer, 0, bytes);

        return bytes;
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

        return timestamp;
    }

    public Duration getPosition() {

    }

    public Duration getDuration() {

    }

    public AudioFormat getOutputFormat() {return outputFormat;}

    public boolean isFinished() {

    }

    @Override
    public void close() {
        clean();
    }
}
