/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.nuked;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import libgme.util.BlipBuffer;
import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.util.DspUtil;
import uk.co.omgdrv.simplevgm.util.Util;

import static uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider.VGM_SAMPLE_RATE_HZ;


/**
 * NukedPsgProvider.
 *
 * @author Federico Berti
 * @version 2019
 * @see "https://forums.nesdev.com/viewtopic.php?f=23&t=15562"
 */
public class NukedPsgProvider implements VgmPsgProvider {

    public static final int PSG_MAX_VOLUME = 0x80;
    public static final int CLOCK_HZ = 3579545;
    public static final int NUKED_PSG_SAMPLING_HZ = CLOCK_HZ / 16;

    private static final double NANOS_TO_SEC = 1_000_000_000;
    private static final double NANOS_PER_SAMPLE = NANOS_TO_SEC / NUKED_PSG_SAMPLING_HZ;
    private static final double NANOS_PER_CYCLE = NANOS_TO_SEC / CLOCK_HZ;

    private final PsgYm7101 psg;
    private final PsgYm7101.PsgContext context;

    private final double[] rawBuffer = new double[NUKED_PSG_SAMPLING_HZ];
    private final double[] resampleBuffer = new double[VGM_SAMPLE_RATE_HZ];
    public final byte[] nukedBuffer = new byte[VGM_SAMPLE_RATE_HZ];

    private double nanosToNextSample = NANOS_PER_SAMPLE;
    private int currentCycle;
    private int sampleCounter = 0;
    public int secondsElapsed = 0;

    protected PsgCompare psgCompare;

    public static NukedPsgProvider createInstance(PsgCompare psgCompare) {
        NukedPsgProvider n = (NukedPsgProvider) VgmPsgProvider.getProvider(NukedPsgProvider.class.getName());
        n.psgCompare = psgCompare;
        return n;
    }

    public NukedPsgProvider() {
        psg = new PsgYm7101Impl();
        context = new PsgYm7101.PsgContext();
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        int delayCycles = toPsgClockCycles(vgmDelayCycles);
        runUntil(delayCycles);
        psg.PSG_Write(context, data);
    }

    @Override
    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {
    }

    @Override
    public void reset() {
        psg.PSG_Reset(context);
    }

    @Override
    public void writeGG(int time, int data) {
    }

    @Override
    public void endFrame(int vgmDelayCycles) {
        long delayCycles = toPsgClockCycles(vgmDelayCycles);
        if (delayCycles > currentCycle) {
            runUntil(vgmDelayCycles);
        }
        currentCycle -= (int) delayCycles;
    }

    private static int toPsgClockCycles(long vgmDelayCycles) {
        return (int) ((vgmDelayCycles * 1.0 / VGM_SAMPLE_RATE_HZ) * CLOCK_HZ);
    }

    private void runUntil(int delayCycles) {
        if (delayCycles > currentCycle) {
            long count = delayCycles;
            while (count-- > 0) {
                psg.PSG_Cycle(context);
                updateSampleBuffer();
            }
            currentCycle = delayCycles;
        }
    }

    protected double rawSample;

    protected boolean updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
        boolean hasSample = false;
        if (nanosToNextSample < 0) {
            hasSample = true;
            nanosToNextSample += NANOS_PER_SAMPLE;
            rawSample = psg.PSG_GetSample(context);
            rawBuffer[sampleCounter] = rawSample;
            sampleCounter++;
            if (sampleCounter == NUKED_PSG_SAMPLING_HZ) {
                sampleCounter = 0;
                DspUtil.fastHpfResample(rawBuffer, resampleBuffer);
                DspUtil.scale8bit(resampleBuffer, nukedBuffer);
//                writeRawData(rawBuffer);
                if (psgCompare != null) {
                    psgCompare.pushData(PsgCompare.PsgType.NUKED, nukedBuffer);
                }
                secondsElapsed++;
            }
        }
        return hasSample;
    }

    final Path rawFile = Paths.get(".", "NUKED_RAW_" + System.currentTimeMillis() + ".raw");

    private void writeRawData(double[] rawBuffer) {
        List<String> l = Arrays.stream(rawBuffer).mapToObj(Double::toString).collect(Collectors.toList());
        Util.writeToFile(rawFile, l);
    }
}
