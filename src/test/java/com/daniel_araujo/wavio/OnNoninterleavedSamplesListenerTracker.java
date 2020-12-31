package com.daniel_araujo.wavio;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class OnNoninterleavedSamplesListenerTracker implements WavReader.OnNoninterleavedSamplesListener {
    public List<ByteBuffer[]> calls = new ArrayList<>();

    /**
     * Controls whether the listener will move the position of the ByteBuffer object to the limit.
     */
    public boolean consume = true;

    public void onNoninterleavedSamples(ByteBuffer[] channels) {
        ArrayList<ByteBuffer> list = new ArrayList<ByteBuffer>();

        for (int i = 0; i < channels.length; i++) {
            ByteBuffer samples = channels[i];

            list.add(i, ByteBufferUtils.deepCopy(samples));

            if (consume) {
                samples.position(samples.limit());
            }
        }

        calls.add(list.toArray(new ByteBuffer[0]));
    }
}
