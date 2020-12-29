# Wavio

Provides a class that can read audio samples from .wav files while being
efficient with memory, careful about performance, and easy to use.

It can read from input streams, ByteBuffer objects or plain byte arrays.

You can choose to receive stereo samples as interleaved or non-interleaved
data.


## Installation

Gradle

```
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.daniel-araujo.wavio:wavio:1.1.0'
}
```

Maven

```
<dependency>
    <groupId>com.daniel-araujo.wavio</groupId>
    <artifactId>wavio</artifactId>
    <version>1.0.0</version>
</dependency>
```


## Example

Access interleaved samples:

```java
FileInputStream input = new FileInputStream("audio_file.wav");

WavReader wav = new WavReader();

wav.setOnInterleavedSamplesListener(new WavReader.OnInterleavedSamplesListener() {
    @Override
    public void onInterleavedSamples(ByteBuffer samples) {
        // Access samples from ByteBuffer. If you don't care about memory
        // consumption, you can copy all samples to a byte array:
        byte[] data = new byte[samples.remaining()];
        samples.get(data);
    }
});

wav.read(input);
```

Access non-interleaved samples:

```java
wav.setOnNoninterleavedSamplesListener(new WavReader.OnNoninterleavedSamplesListener() {
    @Override
    public void onNoninterleavedSamples(ByteBuffer[] channels) {
        for (int i = 0; i < channels.length; i++) {
            // Access samples from channels[i]
            byte[] data = new byte[channels[i].remaining()];
            channels[i].get(data);
        }
    }
});
```


## Contributing

The easiest way to contribute is by starring this project on GitHub!

https://github.com/daniel-araujo/java-wavio

If you've found a bug, would like to suggest a feature or need some help, feel free to create an issue on GitHub:

https://github.com/daniel-araujo/java-wavio/issues


## License

Copyright 2020 Daniel Araujo <contact@daniel-araujo.pt>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
