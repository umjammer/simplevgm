[![Release](https://jitpack.io/v/umjammer/simplevgm.svg)](https://jitpack.io/#umjammer/simplevgm)
[![Java CI](https://github.com/umjammer/simplevgm/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/simplevgm/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/simplevgm/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/simplevgm/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# Simple VGM

 mavenized vgm player

* made it one of the [vavi-sound-emu](https://github.com/umjammer/vavi-sound-emu) spi
* made psg and fm use service provider 

## Install

* [maven](https://jitpack.io/#umjammer/simplevgm)

## Usage

```java
AudioInputStream ais = AudioSystem.getAudioInputStream(Paths.get(vgz).toFile());
Clip clip = AudioSystem.getClip();
clip.open(AudioSystem.getAudioInputStream(new AudioFormat(44100, 16, 2, true, true), ais));
clip.loop(Clip.LOOP_CONTINUOUSLY);
```

## References

 * https://github.com/fedex81/simplevgm
 * https://vgmrips.net/packs/ (vgm download)
 * https://github.com/vlcoo/P3synthVG
 * https://github.com/GhostSonic21/Java_VGMPlayer
 * https://github.com/toyoshim/tss

## TODO

 * ~~merge simplevgm as extended to~~
 * psg providers other than sms doesn't work???

---

# [Original](https://github.com/fedex81/simplevgm)

----------
a (very) simple - Java based - [VGM][1] 1.50 player  

Supported chips:
- TI SN76489
- Yamaha YM2612
- Yamaha YM2413

How To
-----
Download the latest jar file from the releases area  
and run from the command line:  
`java -jar simplevgm-19.0117.jar <file>|<folder>`

Credits
-------
Original GME-VGM implementation:
Shay Green
https://bitbucket.org/mpyne/game-music-emu

Java code lifted from here:
https://github.com/GeoffWilson/VGM

SN76489 PSG implementations:
1. Shay Green: SmsApu
2. Chris White: SN76496.java
3. Chris White: SN76489.java
4. Alexey Khokholov (Nuke.YKT): Nuked-PSG
   https://github.com/nukeykt/Nuked-PSG

YM2413 emulation
Mitsutaka Okazaki  
https://github.com/digital-sound-antiques/emu2413

License
-------

This software is released under a GPL 2.0 license

[1]: https://en.wikipedia.org/wiki/Video_game_music
