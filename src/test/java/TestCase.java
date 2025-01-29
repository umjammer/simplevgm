/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.SourceDataLine;

import libgme.EmuPlayer.Engine;
import libgme.EmuPlayer.JavaEngine;
import libgme.MusicEmu;
import libgme.VGMPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import uk.co.omgdrv.simplevgm.VgmEmu;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.SoundUtil.volume;
import static vavix.util.DelayedWorker.later;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-08-30 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String vgz = "src/test/resources/test.vgm";

    @Property(name = "uk.co.omgdrv.simplevgm.fm")
    String provider = "";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        System.setProperty("uk.co.omgdrv.simplevgm.fm", provider);
Debug.println("volume: " + volume);
    }

    static boolean onIde = System.getProperty("vavi.test", "").equals("ide");
    static long time = onIde ? 1000 * 1000 : 10 * 1000;

    @Test
    void test1() throws Exception {
        System.setProperty("libgme.endless", String.valueOf(onIde));

        VGMPlayer player = new VGMPlayer(VgmEmu.VGM_SAMPLE_RATE_HZ);
        CountDownLatch cdl = new CountDownLatch(1);

        JavaEngine engine = new JavaEngine();
        engine.addLineListener(e -> { if (e.getType() == Type.STOP) cdl.countDown(); });
        engine.setVolume(volume);

Debug.println(vgz);
        player.setEngine(engine);
        player.loadFile(vgz);
        player.startTrack(1);

        cdl.await();
    }

    @Test
    @DisplayName("as spi")
    void test2() throws Exception {
        System.setProperty("libgme.endless", String.valueOf(onIde));

        Path path = Path.of(vgz);
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
                inAudioFormat.getSampleRate(),
                16,
                inAudioFormat.getChannels(),
                true,
                true);
Debug.println("OUT: " + outAudioFormat);

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(pcmAis.getFormat());
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (!later(time).come()) {
            int r = pcmAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    @Test
    @DisplayName("pcm out")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test3() throws Exception {
        VGMPlayer player = new VGMPlayer(VgmEmu.VGM_SAMPLE_RATE_HZ);

        CountDownLatch cdl = new CountDownLatch(1);

        Engine engine = new Engine() { // pcm out

            MusicEmu emu;
            boolean playing;
            OutputStream os;

            @Override
            public void run() {
                try {
                    byte[] buf = new byte[8192];
                    this.playing = true;
                    while(this.playing && !this.emu.trackEnded()) {
                        int count = this.emu.play(buf, buf.length / 2);
                        this.os.write(buf, 0, count * 2);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    cdl.countDown();
                }
            }

            @Override public void setEmu(MusicEmu emu) {
                this.emu = emu;
            }

            @Override public void init() {
                try {
                    os = Files.newOutputStream(Path.of("tmp", "out.pcm"));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override public void reset() {
            }

            @Override public void setVolume(double v) {
            }

            @Override public void setSampleRate(int i) {
            }

            @Override public void stop() {
                playing = false;
            }

            @Override public boolean isPlaying() {
                return playing;
            }

            @Override public void setPlaying(boolean b) {
                this.playing = b;
            }
        };

Debug.println(vgz);
        player.setEngine(engine);
        player.loadFile(vgz);
        player.startTrack(1);

        cdl.await();
    }
}
