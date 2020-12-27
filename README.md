# Wavio

Provides a class that can read audio samples from .wav files.


## Installation

Gradle

```
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.daniel-araujo.wavio:wavio:1.0.0'
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
// Read file.
byte[] input = ...

WavReader wav = new WavReader();

wav.setOnInterleavedSamplesListener(new WavReader.OnInterleavedSamplesListener() {
    @Override
    public void onInterleavedSamples(ByteBuffer samples) {
        // Access samples.
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
