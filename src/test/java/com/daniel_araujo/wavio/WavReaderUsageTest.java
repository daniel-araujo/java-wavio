package com.daniel_araujo.wavio;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class WavReaderUsageTest {
    @Test
    public void readBytesFromInputStream() throws IOException {
        byte[] header = new WavFileHeaderBuilder()
                .setBitsPerSample(16)
                .setChannels(2)
                .setSampleRate(22000)
                .build();
        InputStream file = new ByteArrayInputStream(ArrayUtils.concat(header, new byte [] { 1, 2, 3, 4 }));

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
        InputStream file = new ByteArrayInputStream(ArrayUtils.concat(header, new byte [] { 1, 2, 3, 4 }));

        OnInterleavedSamplesListenerTracker onSamplesListener = new OnInterleavedSamplesListenerTracker();

        WavReader reader = new WavReader();

        reader.setOnInterleavedSamplesListener(onSamplesListener);

        reader.read(file);

        assertEquals(1, onSamplesListener.calls.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, ByteBufferUtils.toArray(onSamplesListener.calls.get(0)));
    }
}
