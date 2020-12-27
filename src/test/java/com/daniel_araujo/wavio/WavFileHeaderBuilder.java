package com.daniel_araujo.wavio;

/**
 * Builds, in layman terms, a wav file header.
 * <p>
 * In technical terms, builds the minimum necessary chunks for a wav file with the exception of the
 * data chunk. The data chunk is partially built, it just contains part id and length. You are meant
 * to append the data yourself.
 */
public class WavFileHeaderBuilder {
    private int channels = 1;

    private int sampleRate = 8000;

    private int bitsPerSample = 16;

    private int dataLength = 0;

    public WavFileHeaderBuilder setDataLength(int length) {
        dataLength = length;
        return this;
    }

    public WavFileHeaderBuilder setChannels(int value) {
        channels = value;
        return this;
    }

    public WavFileHeaderBuilder setSampleRate(int value) {
        sampleRate = value;
        return this;
    }

    public WavFileHeaderBuilder setBitsPerSample(int size) {
        bitsPerSample = size;
        return this;
    }

    public byte[] build() {
        int riffLength = dataLength + 36;
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        int blockAlign = channels * (bitsPerSample / 8);

        byte[] header = new byte[44];

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (riffLength & 0xff);
        header[5] = (byte) ((riffLength >> 8) & 0xff);
        header[6] = (byte) ((riffLength >> 16) & 0xff);
        header[7] = (byte) ((riffLength >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // size of "fmt " chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // audio format
        header[21] = 0;
        header[22] = (byte) ((channels >> 0) & 0xff);
        header[23] = (byte) ((channels >> 8) & 0xff);
        header[24] = (byte) ((sampleRate >> 0) & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) ((byteRate >> 0) & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) ((blockAlign >> 0) & 0xff);
        header[33] = (byte) ((blockAlign >> 8) & 0xff);
        header[34] = (byte) ((bitsPerSample >> 0) & 0xff);
        header[35] = (byte) ((bitsPerSample >> 8) & 0xff);
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) ((dataLength >> 0) & 0xff);
        header[41] = (byte) ((dataLength >> 8) & 0xff);
        header[42] = (byte) ((dataLength >> 16) & 0xff);
        header[43] = (byte) ((dataLength >> 24) & 0xff);

        return header;
    }
}
