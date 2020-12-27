package com.daniel_araujo.wavio;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class WavReaderTest {
    class OnInterleavedSamplesListenerTracker implements WavReader.OnInterleavedSamplesListener {
        public List<ByteBuffer> calls = new ArrayList<ByteBuffer>();

        public void onInterleavedSamples(ByteBuffer samples) {
            calls.add(ByteBufferUtils.deepCopy(samples));
        }
    }

    class OnNoninterleavedSamplesListenerTracker implements WavReader.OnNoninterleavedSamplesListener {
        public List<ByteBuffer[]> calls = new ArrayList<>();

        public void onNoninterleavedSamples(ByteBuffer[] samples) {
            ArrayList<ByteBuffer> list = new ArrayList<ByteBuffer>();

            for (int i = 0; i < samples.length; i++) {
                list.add(i, ByteBufferUtils.deepCopy(samples[i]));
            }

            calls.add(list.toArray(new ByteBuffer[0]));
        }
    }

    @Test(expected = WavReader.ChunkNotFoundException.class)
    public void read_throwsExceptionIfRiffChunkIsMissing() {
        WavReader reader = new WavReader();

        reader.read(new byte[8]);
    }

    @Test(expected = WavReader.ErrorStateException.class)
    public void read_readWillAlwaysThrowExceptionAfterFailingToFindRiffChunk() {
        WavReader reader = new WavReader();

        try {
            reader.read(new byte[8]);
        } catch (Exception ex) {
            // Ignore.
        }

        reader.read(new byte[8]);
    }

    @Test(expected = WavReader.MissingWaveIdentifierException.class)
    public void read_throwsExceptionIfWaveIdentifiedIsMissing() {
        WavReader reader = new WavReader();

        reader.read(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'A', 'V', 'I', ' '});
    }

    @Test(expected = WavReader.MissingFormatSpecificationException.class)
    public void read_throwsExceptionIfFmtChunkIsNotFoundBeforeData() {
        WavReader reader = new WavReader();

        reader.read(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E', 'd', 'a', 't', 'a', 0, 0, 0, 0});
    }

    @Test(expected = WavReader.AudioFormatNotSupportedException.class)
    public void read_throwsExceptionIfAudioFormatIsNotPCM() {
        WavReader reader = new WavReader();

        byte[] data = new WavFileHeaderBuilder().build();

        // Just something else.
        data[20] = 2;

        reader.read(data);
    }

    @Test
    public void read_skipsUnknownChunks() {
        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        byte[] data = new WavFileHeaderBuilder()
                .setBitsPerSample(16)
                .setChannels(2)
                .setSampleRate(22000)
                .build();

        // Change data chunk to pata chunk.
        data[36] = 'p';

        reader.read(data);

        assertEquals(0, onSamplesListener.calls.size());

        // Now provide actual data chunk to see if it will interpret the data correctly.

        reader.read(new byte[]{'d', 'a', 't', 'a', 0, 0, 0, 0, 1, 2, 3, 4});

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
    }

    @Test
    public void read_skipsUnknownChunksWithDataInMultipleReads() {
        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        byte[] data = new WavFileHeaderBuilder()
                .setBitsPerSample(16)
                .setChannels(2)
                .setSampleRate(22000)
                .build();

        // Change data chunk to pata chunk.
        data[36] = 'p';
        data[40] = 4;

        reader.read(data);

        reader.read(new byte[]{1});
        reader.read(new byte[]{2, 3});
        reader.read(new byte[]{4, 'd'});

        assertEquals(0, onSamplesListener.calls.size());

        // Now provide actual data chunk to see if it will interpret the data correctly.

        reader.read(new byte[]{'a', 't', 'a', 0, 0, 0, 0, 1, 2, 3, 4});

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
    }

    @Test
    public void read_skipsUnknownChunksWithoutRemovingNextChunk() {
        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        byte[] data = new WavFileHeaderBuilder()
                .setBitsPerSample(16)
                .setChannels(2)
                .setSampleRate(22000)
                .build();

        // Change data chunk to pata chunk.
        data[36] = 'p';
        data[40] = 4;

        byte[] junk = new byte[]{5, 6, 7, 8};

        byte[] dataChunk = new byte[]{'d', 'a', 't', 'a', 0, 0, 0, 0, 1, 2, 3, 4};

        reader.read(ArrayUtils.concat(data, junk, dataChunk));

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
    }

    @Test
    public void read_parsesFmtChunk() {
        {
            WavReader reader = new WavReader();

            reader.read(
                    new WavFileHeaderBuilder()
                            .setBitsPerSample(16)
                            .setChannels(3)
                            .setSampleRate(22000)
                            .build()
            );

            WavReader.DataFormat format = reader.getDataFormat();

            assertNotNull(format);
            assertEquals(3, format.getChannels());
            assertEquals(22000, format.getSampleRate());
            assertEquals(16, format.getBitsPerSample());
        }

        {
            WavReader reader = new WavReader();

            reader.read(
                    new WavFileHeaderBuilder()
                            .setBitsPerSample(8)
                            .setChannels(1)
                            .setSampleRate(96000)
                            .build()
            );

            WavReader.DataFormat format = reader.getDataFormat();

            assertNotNull(format);
            assertEquals(1, format.getChannels());
            assertEquals(96000, format.getSampleRate());
            assertEquals(8, format.getBitsPerSample());
        }
    }

    @Test
    public void setOnInterleavedSamplesListener_listenerIsCalledWhenNewSamplesAreRead() {
        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        reader.read(
                new WavFileHeaderBuilder()
                        .setBitsPerSample(16)
                        .setChannels(1)
                        .setSampleRate(8000)
                        .build()
        );

        reader.read(new byte[]{1, 2, 3, 4});

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));

        reader.read(new byte[]{5, 6, 7, 8});

        assertEquals(2, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{5, 6, 7, 8}, ByteBufferUtils.toArray(onSamplesListener.calls.get(1)));
    }

    @Test
    public void setOnInterleavedSamplesListener_listenerIsOnlyCalledForCompleteFrames() {
        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        reader.read(
                new WavFileHeaderBuilder()
                        .setBitsPerSample(16)
                        .setChannels(2)
                        .setSampleRate(8000)
                        .build()
        );

        reader.read(new byte[]{1});

        assertEquals(0, onSamplesListener.calls.size());

        reader.read(new byte[]{2, 3});

        assertEquals(0, onSamplesListener.calls.size());

        reader.read(new byte[]{4});

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
    }

    @Test
    public void setOnInterleavedSamplesListener_willCallListenerTwiceInTheSameReadCallIfIncompleteFrameGetsCompletedAndNewFramesAppearAsWell() {
        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        reader.read(
                new WavFileHeaderBuilder()
                        .setBitsPerSample(16)
                        .setChannels(2)
                        .setSampleRate(8000)
                        .build()
        );

        reader.read(new byte[]{1, 2, 3});

        assertEquals(0, onSamplesListener.calls.size());

        reader.read(new byte[]{4, 5, 6, 7, 8});

        assertEquals(2, onSamplesListener.calls.size());

        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
        assertArrayEquals(new byte[]{5, 6, 7, 8}, ByteBufferUtils.toArray(onSamplesListener.calls.get(1)));
    }

    @Test
    public void setOnInterleavedSamplesListener_incompleteFrameAfterCompleteFrame() {
        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        reader.read(
                new WavFileHeaderBuilder()
                        .setBitsPerSample(16)
                        .setChannels(2)
                        .setSampleRate(8000)
                        .build()
        );

        reader.read(new byte[]{1, 2, 3, 4, 5, 6, 7});

        assertEquals(1, onSamplesListener.calls.size());

        reader.read(new byte[]{8});

        assertEquals(2, onSamplesListener.calls.size());

        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
        assertArrayEquals(new byte[]{5, 6, 7, 8}, ByteBufferUtils.toArray(onSamplesListener.calls.get(1)));
    }

    @Test
    public void setOnInterleavedSamplesListener_setNullToRemoveListener() {
        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        reader.read(
                new WavFileHeaderBuilder()
                        .setBitsPerSample(16)
                        .setChannels(1)
                        .setSampleRate(8000)
                        .build()
        );

        reader.read(new byte[]{1, 2, 3, 4});

        assertEquals(1, onSamplesListener.calls.size());

        reader.setOnInterleavedSamplesListener(null);

        reader.read(new byte[]{5, 6, 7, 8});

        assertEquals(1, onSamplesListener.calls.size());
    }

    @Test
    public void setOnNoninterleavedSamplesListener_eachElementInArrayCorrespondingToChannel() {
        OnNoninterleavedSamplesListenerTracker onSamplesListener = new OnNoninterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnNoninterleavedSamplesListener(onSamplesListener);

        reader.read(
                new WavFileHeaderBuilder()
                        .setBitsPerSample(16)
                        .setChannels(2)
                        .setSampleRate(8000)
                        .build()
        );

        reader.read(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});

        assertEquals(1, onSamplesListener.calls.size());
        assertEquals(2, onSamplesListener.calls.get(0).length);
        assertArrayEquals(new byte[]{1, 2, 5, 6}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)[0]));
        assertArrayEquals(new byte[]{3, 4, 7, 8}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)[1]));
    }

    @Test
    public void setOnNoninterleavedSamplesListener_incompleteFrameAfterCompleteFrame() {
        OnNoninterleavedSamplesListenerTracker onSamplesListener = new OnNoninterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnNoninterleavedSamplesListener(onSamplesListener);

        reader.read(
                new WavFileHeaderBuilder()
                        .setBitsPerSample(16)
                        .setChannels(2)
                        .setSampleRate(8000)
                        .build()
        );

        reader.read(new byte[]{1, 2, 3});
        reader.read(new byte[]{4, 5, 6, 7, 8});

        assertEquals(2, onSamplesListener.calls.size());
        assertEquals(2, onSamplesListener.calls.get(0).length);
        assertEquals(2, onSamplesListener.calls.get(1).length);
        assertArrayEquals(new byte[]{1, 2}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)[0]));
        assertArrayEquals(new byte[]{3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)[1]));
        assertArrayEquals(new byte[]{5, 6}, ByteBufferUtils.toArray(onSamplesListener.calls.get(1)[0]));
        assertArrayEquals(new byte[]{7, 8}, ByteBufferUtils.toArray(onSamplesListener.calls.get(1)[1]));
    }

    @Test
    public void bugfix_read_doesNotCrashWhenSkippingUnknownChunkWithNotAllDataInTheSameRead() {
        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        byte[] data = new WavFileHeaderBuilder()
                .setBitsPerSample(16)
                .setChannels(2)
                .setSampleRate(22000)
                .build();

        // Change data chunk to pata chunk.
        data[36] = 'p';
        data[40] = 4;

        reader.read(data);

        reader.read(new byte[]{1});
        reader.read(new byte[]{2, 3, 4});

        assertEquals(0, onSamplesListener.calls.size());

        // Now provide actual data chunk to see if it will interpret the data correctly.

        reader.read(new byte[]{'d', 'a', 't', 'a', 0, 0, 0, 0, 1, 2, 3, 4});

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
    }
}
