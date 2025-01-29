[![Release](https://jitpack.io/v/umjammer/simplevgm.svg)](https://jitpack.io/#umjammer/simplevgm)
[![Java CI](https://github.com/umjammer/simplevgm/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/simplevgm/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/simplevgm/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/simplevgm/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# Simple VGM

 mavenized vgm player

* made it one of the [vavi-sound-emu](https://github.com/umjammer/vavi-sound-emu) spi
* made *psg* and *fm* use service provider 

| name      | common name | type | status | comment                  |
|-----------|-------------|------|:------:|--------------------------|
| Ym2612    | OPN2        | FM   |   ✅️   | mame:dallongeville+green |
| Ym3438    | OPN2 (cmos) | FM   |   ✅️   | nukeykt                  |
| Ym2612    | OPN2        | FM   |   ✅️   | MDSound                  |
| Ym2413    | OPLL        | FM   |   ✅️   | okaxaki                  |
| Sn76489   |             | PSG  |  ✅ 🚧  | green                    |
| Sn76489   |             | PSG  |        | javageer:white           |
| Sn76496   |             | PSG  |        | javageer-2:white         |
| Ym7101    |             | PSG  |        | nukeykt                  |

## Install

* [maven](https://jitpack.io/#umjammer/simplevgm)

## Usage

```java
  AudioInputStream vgmAis = AudioSystem.getAudioInputStream(Paths.get(vgm).toFile());
  AudioFormat inFormat = sourceAis.getFormat();
  AudioFormat outFormat = new AudioFormat(inFormat.getSampleRate(), 16, inFormat.getChannels(), true, true, props);
  AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outFormat, vgmAis);
  SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, pcmAis.getFormat()));
  line.open(pcmAis.getFormat());
  line.start();
  byte[] buffer = new byte[line.getBufferSize()];
  int bytesRead;
  while ((bytesRead = pcmAis.read(buffer)) != -1) {
    line.write(buffer, 0, bytesRead);
  }
  line.drain();
```

### properties for target `AudioFormat`

 * `track` ... specify track # in the file to play

### system properties

 * `libgme.endless` ... loop audio playing or not, default `false`
 * `uk.co.omgdrv.simplevgm.psg` ... a class name extends `uk.co.omgdrv.simplevgm.model.VgmPsgProvider`
 * `uk.co.omgdrv.simplevgm.fm` ... a class name extends `uk.co.omgdrv.simplevgm.model.VgmFmProvider`


## References

 * https://github.com/fedex81/simplevgm
 * https://vgmrips.net/packs/ (vgm download)
 * https://github.com/vlcoo/P3synthVG
 * https://github.com/GhostSonic21/Java_VGMPlayer
 * https://github.com/toyoshim/tss
 * https://sourceforge.net/projects/javagear/
 * https://github.com/abbruzze/sega-md
 * https://github.com/abbruzze/sega-md/blob/main/src/ucesoft/smd/audio/FM.scala (nuke usage)
 * psg
   * https://github.com/ShreyasTheRag/PSG-audio
   * https://github.com/GhostSonic21/Java_VGMPlayer
   * https://github.com/gittymo/BBCSoundEditor
 * https://github.com/fedex81/helios
 * https://github.com/wide-dot/6809-game-builder

## TODO

 * ~~merge simplevgm as extended to~~
 * psg has buffering engine? (so at least one psg instance is needed)
 * psg providers other than sms doesn't work???
 * Sn76489(green) works alone but doesn't work as a spi

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
