/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.sound.sampled.AudioFormat;

import libgme.util.BlipBuffer;
import uk.co.omgdrv.simplevgm.VgmEmu;
import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.gear.GearPsgProvider;
import uk.co.omgdrv.simplevgm.psg.gear2.Gear2PsgProvider;
import uk.co.omgdrv.simplevgm.psg.green.GreenPsgProvider;
import uk.co.omgdrv.simplevgm.psg.green.SmsApu;
import uk.co.omgdrv.simplevgm.psg.nuked.BlipNukedPsgProvider;
import uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider;
import uk.co.omgdrv.simplevgm.util.Util;

import static java.lang.System.getLogger;
import static uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider.VGM_SAMPLE_RATE_HZ;


/**
 * PsgCompare.
 *
 * @author Federico Berti
 * @version 2019
 */
public class PsgCompare implements VgmPsgProvider {

    private static final Logger logger = getLogger(PsgCompare.class.getName());

    private static final int RUN_FOR_SECONDS = 30;
    private static final boolean WRITE_FILE = false;

    public enum PsgType {
        GEAR,
        GEAR2,
        NUKED,
        NUKED_FILTER,
        NUKED_BLIP,
        GREEN
    }

    private static final boolean SIGNED = true;
    public static final AudioFormat audioFormat8bit =
            new AudioFormat(VGM_SAMPLE_RATE_HZ, 8, 1, SIGNED, false);
    private static final AudioFormat audioFormat16bit =
            new AudioFormat(VGM_SAMPLE_RATE_HZ, 16, 1, SIGNED, false);

    private static final Path nukeFile = Paths.get("Nuke_" + System.currentTimeMillis() + ".raw");
    private static final Path nukeFilterFile = Paths.get("NukeFilter_" + System.currentTimeMillis() + ".raw");
    private static final Path nukeBlipFile = Paths.get("NukeBlip_" + System.currentTimeMillis() + ".raw");
    private static final Path gearFile = Paths.get("Gear_" + System.currentTimeMillis() + ".raw");
    private static final Path gearFile2 = Paths.get("Gear2_" + System.currentTimeMillis() + ".raw");
    private static final Path greenFile = Paths.get("Green_" + System.currentTimeMillis() + ".raw");

    private final GearPsgProvider gearPsg;
    private final Gear2PsgProvider gear2Psg;
    private final NukedPsgProvider nukePsg;
    private final GreenPsgProvider greenPsg;
    private final BlipNukedPsgProvider blipNukedPsg;

    private final SmsApu vgmEmuPsg;

    public PsgCompare() {
        this.gearPsg = GearPsgProvider.createInstance(this);
        this.gear2Psg = Gear2PsgProvider.createInstance(this);
        this.nukePsg = NukedPsgProvider.createInstance(this);
        this.blipNukedPsg = BlipNukedPsgProvider.createInstance(this);
        this.greenPsg = GreenPsgProvider.createInstance(this);

        this.vgmEmuPsg = SmsApu.getInstance();
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        nukePsg.writeData(vgmDelayCycles, data);
        gearPsg.writeData(vgmDelayCycles, data);
        gear2Psg.writeData(vgmDelayCycles, data);
        blipNukedPsg.writeData(vgmDelayCycles, data);
        greenPsg.writeData(vgmDelayCycles, data);

        vgmEmuPsg.writeData(VgmEmu.toPSGTimeGreen(vgmDelayCycles), data);
    }

    @Override
    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {
        vgmEmuPsg.setOutput(center, left, right);
    }

    @Override
    public void reset() {
        nukePsg.reset();
        gearPsg.reset();
        greenPsg.reset();
        gear2Psg.reset();
        blipNukedPsg.reset();
    }

    @Override
    public void writeGG(int time, int data) {
        vgmEmuPsg.writeGG(VgmEmu.toPSGTimeGreen(time), data);
    }

    @Override
    public void endFrame(int vgmDelayCycles) {
        nukePsg.endFrame(vgmDelayCycles);
        gearPsg.endFrame(vgmDelayCycles);
        gear2Psg.endFrame(vgmDelayCycles);
        blipNukedPsg.endFrame(vgmDelayCycles);
        greenPsg.endFrame(vgmDelayCycles);

        vgmEmuPsg.endFrame(VgmEmu.toPSGTimeGreen(vgmDelayCycles));
    }

    private void checkIntervalDone() {
//logger.log(Level.TRACE, "Seconds: " + nukePsg.secondsElapsed);
        if (nukePsg.secondsElapsed >= RUN_FOR_SECONDS) {
logger.log(Level.DEBUG, "Stopping after: " + RUN_FOR_SECONDS + " seconds");
            main(null);
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        try {
            List<Path> files = Files.list(Paths.get(".")).
                    filter(f -> f.toString().endsWith(".raw")).toList();
            files.forEach(f -> Util.convertToWav(f.getFileName().toString(), audioFormat8bit));
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    public void pushData(PsgType type, byte[] buffer) {
        if (WRITE_FILE) {
            switch (type) {
                case GEAR:
                    Util.writeToFile(gearFile, buffer);
                    break;
                case GEAR2:
//                Util.writeToFile(gearFile2, buffer);
                    break;
                case GREEN:
                    // TODO this is 16bit
//                Util.writeToFile(greenFile, buffer);
                    break;
                case NUKED:
                    Util.writeToFile(nukeFile, buffer);
                    break;
                case NUKED_BLIP:
//                Util.writeToFile(nukeBlipFile, buffer);
                    break;
                case NUKED_FILTER:
//                Util.writeToFile(nukeFilterFile, buffer);
                    break;
            }
        }
        checkIntervalDone();
    }
}
