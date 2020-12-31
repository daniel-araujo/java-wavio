package com.daniel_araujo.wavio;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class OnInterleavedSamplesListenerTracker implements WavReader.OnInterleavedSamplesListener {
    public List<ByteBuffer> calls = new ArrayList<ByteBuffer>();

    /**
     * Controls whether the listener will move the position of the ByteBuffer object to the limit.
     */
    public boolean consume = true;

    public void onInterleavedSamples(ByteBuffer samples) {
        calls.add(ByteBufferUtils.deepCopy(samples));

        if (consume) {
            samples.position(samples.limit());
        }
    }
}
