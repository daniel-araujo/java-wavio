package com.daniel_araujo.wavio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Reads wave file and extracts PCM samples. This implementation does not respect the data length
 * and will read samples indefinitely. Also only works with PCM audio format.
 */
public class WavReader {
    /**
     * The state that the reader is in so it always knows what to do with incoming data.
     */
    private State state;

    /**
     * Set to true when riff chunk has been found.
     */
    private boolean hasFoundRiffWaveChunk;

    /**
     * Data format. This allows you to know how to interpret sample data.
     */
    private DataFormat format;

    /**
     * Listener that will receive interleaved samples.
     */
    private OnInterleavedSamplesListener onInterleavedSamplesListener;

    /**
     * Listener that will receive interleaved samples.
     */
    private OnNoninterleavedSamplesListener onNoninterleavedSamplesListener;

    /**
     * Creates a new reader. Expects to read a file from the start.
     */
    public WavReader() {
        state = new StateReadNextChunkHeader();
    }

    /**
     * Reads data. Will call appropriate listeners as contents are parsed.
     *
     * @param input
     */
    public void read(byte[] input) {
        process(ByteBuffer.wrap(input));
    }

    /**
     * @return Sample format.
     */
    public DataFormat getDataFormat() {
        return format;
    }

    /**
     * Registers a listener that will receive interleaved samples.
     *
     * @param listener Can be null to existing listener.
     */
    public void setOnInterleavedSamplesListener(OnInterleavedSamplesListener listener) {
        onInterleavedSamplesListener = listener;
    }

    /**
     * Registers a listener that will receive non-interleaved samples.
     *
     * @param listener Can be null to existing listener.
     */
    public void setOnNoninterleavedSamplesListener(OnNoninterleavedSamplesListener listener) {
        onNoninterleavedSamplesListener = listener;
    }

    /**
     * Interface for receiving interleaved samples.
     */
    public interface OnInterleavedSamplesListener {
        /**
         * Receives samples.
         *
         * @param samples
         */
        void onInterleavedSamples(ByteBuffer samples);
    }

    /**
     * Interface for receiving non-interleaved samples.
     */
    public interface OnNoninterleavedSamplesListener {
        /**
         * Receives samples.
         *
         * @param samples Each element in the array contains the samples for the corresponding channel.
         */
        void onNoninterleavedSamples(ByteBuffer[] samples);
    }

    /**
     * Does all the parsing.
     *
     * @param input
     */
    private void process(ByteBuffer input) {
        if (state instanceof StateReadNextChunkHeader) {
            StateReadNextChunkHeader stateImpl = (StateReadNextChunkHeader) state;

            if (!readInputUntilReachingPosition(input, stateImpl.data, 8)) {
                // Not enough data.
                return;
            }

            stateImpl.data.position(0);
            RiffUtils.ChunkHeader chunkHeader = RiffUtils.parseChunkHeader(stateImpl.data);

            state = new StateInterpretChunkHeader(chunkHeader);

            // Continue.
            process(input);
        } else if (state instanceof StateInterpretChunkHeader) {
            StateInterpretChunkHeader stateImpl = (StateInterpretChunkHeader) state;

            if (stateImpl.header.typeId.equals("RIFF")) {
                state = new StateRiffChunkIdentifier();
            } else if (stateImpl.header.typeId.equals("fmt ")) {
                state = new StateFmtChunk(stateImpl.header);
            } else if (stateImpl.header.typeId.equals("data")) {
                if (format != null) {
                    state = new StateDataSamples(stateImpl.header);
                } else {
                    state = new StateError();
                    throw new MissingFormatSpecificationException();
                }
            } else {
                if (hasFoundRiffWaveChunk) {
                    state = new StateSkipChunk(stateImpl.header);
                } else {
                    state = new StateError();
                    throw new ChunkNotFoundException("RIFF");
                }
            }

            // Continue.
            process(input);
        } else if (state instanceof StateRiffChunkIdentifier) {
            StateRiffChunkIdentifier stateImpl = (StateRiffChunkIdentifier) state;

            if (!readInputUntilReachingPosition(input, stateImpl.data, 4)) {
                // Not enough data.
                return;
            }

            stateImpl.data.position(0);

            byte[] identifierChars = new byte[4];
            stateImpl.data.get(identifierChars);

            if (!new String(identifierChars).equals("WAVE")) {
                state = new StateError();
                throw new MissingWaveIdentifierException();
            }

            hasFoundRiffWaveChunk = true;

            state = new StateReadNextChunkHeader();

            // Continue.
            process(input);
        } else if (state instanceof StateSkipChunk) {
            StateSkipChunk stateImpl = (StateSkipChunk) state;

            if (input.remaining() == 0) {
                return;
            }

            int toSkip = stateImpl.header.length - stateImpl.skipped;

            if (toSkip > input.remaining()) {
                toSkip = input.remaining();
            }

            input.position(input.position() + toSkip);

            stateImpl.skipped += toSkip;

            if (stateImpl.skipped == stateImpl.header.length) {
                state = new StateReadNextChunkHeader();
            }

            process(input);
        } else if (state instanceof StateFmtChunk) {
            StateFmtChunk stateImpl = (StateFmtChunk) state;

            if (!readInputUntilReachingPosition(input, stateImpl.data, stateImpl.header.length)) {
                // Not enough data.
                return;
            }

            stateImpl.data.position(0);

            DataFormat format = new DataFormat();
            int audioFormat = stateImpl.data.getShort();

            if (audioFormat != 1) {
                throw new AudioFormatNotSupportedException();
            }

            format.channels = stateImpl.data.getShort();
            format.sampleRate = stateImpl.data.getInt();
            int byteRate = stateImpl.data.getInt();
            int blockAlign = stateImpl.data.getShort();
            format.bitsPerSample = stateImpl.data.getShort();

            this.format = format;

            state = new StateReadNextChunkHeader();

            // Continue.
            process(input);
        } else if (state instanceof StateDataSamples) {
            StateDataSamples stateImpl = (StateDataSamples) state;

            int frameSize = getFrameSize();

            if (stateImpl.incompleteFrame.position() > 0) {
                // We have an incomplete frame.

                int missingBytes = frameSize - stateImpl.incompleteFrame.position();

                if (input.remaining() >= missingBytes) {
                    input.position(input.position() + missingBytes);

                    ByteBuffer remainingFrame = input.duplicate();
                    remainingFrame.limit(remainingFrame.position());
                    remainingFrame.position(remainingFrame.position() - missingBytes);
                    stateImpl.incompleteFrame.put(remainingFrame);

                    stateImpl.incompleteFrame.position(0);
                    stateImpl.incompleteFrame.limit(frameSize);

                    onSamples(stateImpl.incompleteFrame);
                } else {
                    stateImpl.incompleteFrame.put(input);
                    // Not enough data.
                    return;
                }
            }

            if (input.remaining() > 0) {
                int incompleteFrameSize = input.remaining() % frameSize;

                int available = input.remaining() - incompleteFrameSize;

                if (available > 0) {
                    ByteBuffer samples = input.duplicate();
                    samples.limit(samples.limit() - incompleteFrameSize);

                    onSamples(samples);
                }

                if (incompleteFrameSize > 0) {
                    input.position(input.limit() - incompleteFrameSize);
                    stateImpl.incompleteFrame.put(input);
                }
            }
        } else if (state instanceof StateError) {
            throw new ErrorStateException();
        } else {
            throw new RuntimeException("Not implemented.");
        }
    }

    /**
     * Gets data from input and puts in holder until holder reaches given position.
     *
     * @param input
     * @param holder
     * @param position
     * @return True if reached position, false otherwise.
     */
    private boolean readInputUntilReachingPosition(ByteBuffer input, ByteBuffer holder, int position) {
        while (holder.position() < position) {
            if (!input.hasRemaining()) {
                // No more data available.
                return false;
            }

            holder.put(input.get());
        }

        return holder.position() >= position;
    }

    /**
     * Returns frame size based on sample format.
     *
     * @return
     */
    private int getFrameSize() {
        return format.getBytesPerSample() * format.getChannels();
    }

    /**
     * Dispatches samples listeners.
     *
     * @param samples
     */
    private void onSamples(ByteBuffer samples) {
        if (onInterleavedSamplesListener != null) {
            onInterleavedSamplesListener.onInterleavedSamples(samples);
        }

        if (onNoninterleavedSamplesListener != null) {
            final int channels = format.getChannels();
            final int frameSize = getFrameSize();
            final int bytesPerSample = format.getBytesPerSample();
            final int samplesPerChannel = samples.remaining() / frameSize;

            ByteBuffer[] noninterleavedSamples = new ByteBuffer[channels];

            for (int c = 0; c < channels; c++) {
                ByteBuffer channelBuffer = ByteBuffer.allocate(samplesPerChannel * bytesPerSample);

                for (int i = 0; i < samplesPerChannel; i++) {
                    for (int b = 0; b < bytesPerSample; b++) {
                        final int interleavedIndex = i * frameSize + c * bytesPerSample + b;
                        final int noninterleavedIndex = i * bytesPerSample + b;

                        channelBuffer.put(noninterleavedIndex, samples.get(samples.position() + interleavedIndex));
                    }
                }

                noninterleavedSamples[c] = channelBuffer;
            }

            onNoninterleavedSamplesListener.onNoninterleavedSamples(noninterleavedSamples);
        }
    }

    /**
     * Lets us know how samples are laid out in the wav file.
     */
    public class DataFormat {
        private int sampleRate;

        private int channels;

        private int bitsPerSample;

        public int getBytesPerSample() {
            return (int) Math.ceil(bitsPerSample / 8.0);
        }

        public int getBitsPerSample() {
            return bitsPerSample;
        }

        public int getChannels() {
            return channels;
        }

        public int getSampleRate() {
            return sampleRate;
        }
    }

    /**
     * Base class for all exceptions.
     */
    public static class Exception extends RuntimeException {
        Exception() {
            super();
        }

        Exception(String message) {
            super(message);
        }
    }

    public static class ChunkNotFoundException extends Exception {
        ChunkNotFoundException(String typeId) {
            super("Chunk with type id " + typeId + " not found.");
        }
    }

    public static class MissingFormatSpecificationException extends Exception {
    }

    public static class AudioFormatNotSupportedException extends Exception {
    }

    public static class ErrorStateException extends Exception {
    }

    public static class MissingWaveIdentifierException extends Exception {
    }

    /**
     * Base class for all states.
     */
    private static class State {
    }

    /**
     * An error has occurred and the reader can no longer function.
     */
    private static class StateError extends State {
    }

    /**
     * Parses next chunk header.
     */
    private static class StateReadNextChunkHeader extends State {
        /**
         * Data that we're accumulating to be able to parse an entire RIFF header.
         */
        public ByteBuffer data;

        StateReadNextChunkHeader() {
            data = ByteBuffer.allocate(8);
            data.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    /**
     * Decides what to do with a chunk.
     */
    private static class StateInterpretChunkHeader extends State {
        /**
         * Chunk header that will be scrutinized.
         */
        public RiffUtils.ChunkHeader header;

        StateInterpretChunkHeader(RiffUtils.ChunkHeader header) {
            this.header = header;
        }
    }

    /**
     * Skips all the data of the current chunk.
     */
    private static class StateSkipChunk extends State {
        /**
         * Unknown chunk's header.
         */
        public RiffUtils.ChunkHeader header;

        /**
         * How many bytes have been skipped so far.
         */
        public int skipped;

        StateSkipChunk(RiffUtils.ChunkHeader header) {
            this.header = header;
            skipped = 0;
        }
    }

    /**
     * Parsing wave identification in riff chunk.
     */
    private static class StateRiffChunkIdentifier extends State {
        /**
         * Data that we're accumulating.
         */
        public ByteBuffer data;

        StateRiffChunkIdentifier() {
            data = ByteBuffer.allocate(4);
            data.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    /**
     * Parsing fmt chunk.
     */
    private static class StateFmtChunk extends State {
        /**
         * Fmt chunk header.
         */
        public RiffUtils.ChunkHeader header;

        /**
         * Data that we're accumulating to be able to parse an entire FMT chunk for PCM data.
         */
        public ByteBuffer data;

        StateFmtChunk(RiffUtils.ChunkHeader header) {
            this.header = header;
            data = ByteBuffer.allocate(16);
            data.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    /**
     * Parsing data samples.
     */
    private static class StateDataSamples extends State {
        /**
         * Data chunk header.
         */
        public RiffUtils.ChunkHeader header;

        /**
         * An incomplete frame from the last read. This will be appended to the data of the next read.
         */
        public ByteBuffer incompleteFrame;

        StateDataSamples(RiffUtils.ChunkHeader header) {
            this.header = header;
            // That ought to be enough.
            incompleteFrame = ByteBuffer.allocate(128);
            incompleteFrame.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    private static abstract class RiffUtils {
        /**
         * All chunks have the following format:
         * <p>
         * 4 bytes: an ASCII identifier for this chunk (examples are "fmt " and "data"; note the space in "fmt ").
         * 4 bytes: an unsigned, little-endian 32-bit integer with the length of this chunk (except this field itself and the chunk identifier).
         * variable-sized field: the chunk data itself, of the size given in the previous field.
         * a pad byte, if the chunk's length is not even.
         *
         * @param buffer Buffer to read data from. Must be able to read at least 8 bytes.
         */
        public static ChunkHeader parseChunkHeader(ByteBuffer buffer) {
            // Obligatory.
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            ChunkHeader header = new ChunkHeader();

            byte[] identifierChars = new byte[4];
            buffer.get(identifierChars);

            header.typeId = new String(identifierChars);
            header.length = buffer.getInt();

            return header;
        }

        /**
         * A parsed chunk header.
         */
        public static class ChunkHeader {
            String typeId;
            int length;
        }
    }
}
