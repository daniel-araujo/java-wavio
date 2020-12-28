package com.daniel_araujo.wavio;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class WavReaderUsageTest {
    @Test
    public void readBytesFromInputStream() throws IOException {
        byte[] header = new WavFileHeaderBuilder()
                .setBitsPerSample(16)
                .setChannels(2)
                .setSampleRate(22000)
                .build();
        InputStream file = new ByteArrayInputStream(ArrayUtils.concat(header, new byte[]{1, 2, 3, 4}));

        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        byte[] buffer = new byte[4];
        int length = 0;
        while ((length = file.read(buffer)) != -1) {
            reader.read(buffer, 0, length);
        }

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
    }

    @Test
    public void readEntireInputStream() throws IOException {
        byte[] header = new WavFileHeaderBuilder()
                .setBitsPerSample(16)
                .setChannels(2)
                .setSampleRate(22000)
                .build();
        InputStream file = new ByteArrayInputStream(ArrayUtils.concat(header, new byte[]{1, 2, 3, 4}));

        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        reader.read(file);

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
    }

    @Test
    public void readFromSeveralByteArrays() throws IOException {
        byte[] header = new WavFileHeaderBuilder()
                .setBitsPerSample(16)
                .setChannels(2)
                .setSampleRate(22000)
                .build();

        byte[] data1 = new byte[]{1, 2, 3, 4};
        byte[] data2 = new byte[]{5, 6, 7, 8};
        byte[] data3 = new byte[]{9, 10, 11, 12};

        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        reader.read(header);
        reader.read(data1);
        reader.read(data2);
        reader.read(data3);

        assertEquals(3, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
        assertArrayEquals(new byte[]{5, 6, 7, 8}, ByteBufferUtils.toArray(onSamplesListener.calls.get(1)));
        assertArrayEquals(new byte[]{9, 10, 11, 12}, ByteBufferUtils.toArray(onSamplesListener.calls.get(2)));
    }

    @Test
    public void readFromByteBuffer() throws IOException {
        byte[] header = new WavFileHeaderBuilder()
                .setBitsPerSample(16)
                .setChannels(2)
                .setSampleRate(22000)
                .build();
        ByteBuffer buffer = ByteBuffer.wrap(ArrayUtils.concat(header, new byte[]{1, 2, 3, 4}));

        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        reader.read(buffer);

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
    }
}
