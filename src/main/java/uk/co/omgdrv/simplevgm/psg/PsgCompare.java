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

    public GearPsgProvider createGearPsg(PsgCompare compare) {
        GearPsgProvider g = (GearPsgProvider) VgmPsgProvider.getProvider(GearPsgProvider.class.getName());
        g.addComparator(buf -> pushData(GearPsgProvider.class, buf));
        return g;
    }

    public Gear2PsgProvider createGear2Psg(PsgCompare compare) {
        Gear2PsgProvider g = (Gear2PsgProvider) VgmPsgProvider.getProvider(Gear2PsgProvider.class.getName());
        g.addComparator(buf -> pushData(Gear2PsgProvider.class, buf));
        return g;
    }

    public NukedPsgProvider createNukedPsg(PsgCompare psgCompare) {
        NukedPsgProvider n = (NukedPsgProvider) VgmPsgProvider.getProvider(NukedPsgProvider.class.getName());
        n.addComparator(buf -> pushData(NukedPsgProvider.class, buf));
        return n;
    }

    public BlipNukedPsgProvider createBlipNuked(PsgCompare psgCompare) {
        BlipNukedPsgProvider n = (BlipNukedPsgProvider) VgmPsgProvider.getProvider(BlipNukedPsgProvider.class.getName());
        n.addComparator(buf -> pushData(BlipNukedPsgProvider.class, buf));
        return n;
    }

    public GreenPsgProvider createGreenPsg(PsgCompare compare) {
        GreenPsgProvider g = (GreenPsgProvider) VgmPsgProvider.getProvider(GreenPsgProvider.class.getName());
        g.addComparator(buf -> pushData(GreenPsgProvider.class, buf));
        return g;
    }

    private final GreenPsgProvider vgmEmuPsg;

    public PsgCompare() {
        this.gearPsg = createGearPsg(this);
        this.gear2Psg = createGear2Psg(this);
        this.nukePsg = createNukedPsg(this);
        this.blipNukedPsg = createBlipNuked(this);
        this.greenPsg = createGreenPsg(this);

        this.vgmEmuPsg = greenPsg;
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        nukePsg.writeData(vgmDelayCycles, data);
        gearPsg.writeData(vgmDelayCycles, data);
        gear2Psg.writeData(vgmDelayCycles, data);
        blipNukedPsg.writeData(vgmDelayCycles, data);
        gear2Psg.writeData(vgmDelayCycles, data);

        vgmEmuPsg.writeData((int) gear2Psg.toPsgCycles(vgmDelayCycles), data);
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
        vgmEmuPsg.writeGG((int) gear2Psg.toPsgCycles(time), data);
    }

    @Override
    public void endFrame(int vgmDelayCycles) {
        nukePsg.endFrame(vgmDelayCycles);
        gearPsg.endFrame(vgmDelayCycles);
        gear2Psg.endFrame(vgmDelayCycles);
        blipNukedPsg.endFrame(vgmDelayCycles);
        greenPsg.endFrame(vgmDelayCycles);

        vgmEmuPsg.endFrame((int) toPsgCycles(vgmDelayCycles));
    }

    @Override
    public long toPsgCycles(long vgmDelayCycles) {
        return gear2Psg.toPsgCycles(vgmDelayCycles);
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

    public void pushData(Class<? extends VgmPsgProvider> type, byte[] buffer) {
        if (WRITE_FILE) {
            switch (type.getSimpleName()) {
                case "GearPsgProvider":
                    Util.writeToFile(gearFile, buffer);
                    break;
                case "Gear2PsgProvider":
//                Util.writeToFile(gearFile2, buffer);
                    break;
                case "GreenPsgProvider":
                    // TODO this is 16bit
//                Util.writeToFile(greenFile, buffer);
                    break;
                case "NukedPsgProvider":
                    Util.writeToFile(nukeFile, buffer);
                    break;
                case "BlipNukedPsgProvider":
//                Util.writeToFile(nukeBlipFile, buffer);
                    break;
                case "NUKED_FILTER":
//                Util.writeToFile(nukeFilterFile, buffer);
                    break;
            }
        }
        checkIntervalDone();
    }
}
