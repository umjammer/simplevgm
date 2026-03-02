import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import libgme.EmuPlayer.Engine;
import libgme.MusicEmu;
import libgme.VGMPlayer;
import uk.co.omgdrv.simplevgm.VgmEmu;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavix.util.DelayedWorker.later;


@PropsEntity(url = "file:local.properties")
class PsgProviderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String psg;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "uk.co.omgdrv.simplevgm.psg.gear.GearPsgProvider",
            "uk.co.omgdrv.simplevgm.psg.gear2.Gear2PsgProvider",
            "uk.co.omgdrv.simplevgm.psg.green.GreenPsgProvider",
            "uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider"
    })
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testAllPsgProviders(String provider) throws Exception {

        AtomicBoolean hasSound = new AtomicBoolean();

        System.out.println("Testing provider: " + provider);
        System.setProperty("uk.co.omgdrv.simplevgm.psg", provider);

        VGMPlayer player = new VGMPlayer(VgmEmu.VGM_SAMPLE_RATE_HZ);
        CountDownLatch cdl = new CountDownLatch(1);

        Engine engine = new Engine() {
            MusicEmu emu;
            boolean playing;
            long samplesDecoded = 0;

            @Override
            public void run() {
                try {
                    byte[] buf = new byte[8192];
                    this.playing = true;
                    // Decode 1 second
                    while(!later(2 * 1000).come() && this.playing && !this.emu.trackEnded() && samplesDecoded < VgmEmu.VGM_SAMPLE_RATE_HZ) {
                        int count = this.emu.play(buf, buf.length / 2);
                        samplesDecoded += count;

                        // Check if sound buffer is not completely empty to prove it's producing sound
                        for (int i = 0; i < count; i++) {
                            if (buf[i] != 0) {
                                hasSound.set(true);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    cdl.countDown();
                }
            }

            @Override public void setEmu(MusicEmu emu) { this.emu = emu; }
            @Override public void init() {}
            @Override public void reset() {}
            @Override public void setVolume(double v) {}
            @Override public void setSampleRate(int i) {}
            @Override public void stop() { playing = false; }
            @Override public boolean isPlaying() { return playing; }
            @Override public void setPlaying(boolean b) { this.playing = b; }
        };

        player.setEngine(engine);
        player.loadFile(psg);
        player.startTrack(1);

        cdl.await();

        assertTrue(hasSound.get());
    }
}
