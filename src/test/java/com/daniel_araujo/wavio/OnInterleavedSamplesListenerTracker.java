package com.daniel_araujo.wavio;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class OnInterleavedSamplesListenerTracker implements WavReader.OnInterleavedSamplesListener {
    public List<ByteBuffer> calls = new ArrayList<ByteBuffer>();

    public void onInterleavedSamples(ByteBuffer samples) {
        calls.add(ByteBufferUtils.deepCopy(samples));
    }
}
