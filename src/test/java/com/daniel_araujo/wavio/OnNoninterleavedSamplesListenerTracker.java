package com.daniel_araujo.wavio;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
