/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.nuked;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;
import uk.co.omgdrv.simplevgm.util.DspUtil;
import uk.co.omgdrv.simplevgm.util.Util;


/**
 * NukedPsgProvider.
 *
 * @author Federico Berti
 * @version 2019
 * @see "https://forums.nesdev.com/viewtopic.php?f=23&t=15562"
 */
public class NukedPsgProvider extends BaseVgmPsgProvider {

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

    public NukedPsgProvider() {
        psg = new PsgYm7101Impl();
        context = new PsgYm7101.PsgContext();
    }

    @Override
    public void writeData(int clockTime, int data) {
        // clockTime is already PSG clock cycles (from VgmEmu.toPSGTime)
        // Do NOT double-convert via toPsgCycles
        runUntil(clockTime);
        psg.PSG_Write(context, data);
    }

    @Override
    public void reset() {
        psg.PSG_Reset(context);
    }

    @Override
    public void endFrame(int clockEndTime) {
        // clockEndTime is already PSG clock cycles
        if (clockEndTime > currentCycle) {
            runUntil(clockEndTime);
        }
        currentCycle -= clockEndTime;
    }

    @Override
    public void runUntil(int targetCycle) {
        while (currentCycle < targetCycle) {
            // PSG_Cycle advances one of 4 channels per call (÷4 via rotation).
            // SN76489 needs ÷16 total, so call PSG_Cycle every 4th clock: 4 × 4 = 16.
            if ((currentCycle & 3) == 0) {
                psg.PSG_Cycle(context);
            }
            updateSampleBuffer();
            currentCycle++;
        }
    }

    protected double rawSample;
    private int lastSample = 0;

    @Override
    public void updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
//        boolean hasSample = false;
        if (nanosToNextSample < 0) {
//            hasSample = true;
            nanosToNextSample += NANOS_PER_SAMPLE;
            // PSG_GetSample is a pure read — PSG_Cycle in the main loop already advances state
            rawSample = psg.PSG_GetSample(context);
            rawBuffer[sampleCounter] = rawSample;
            sampleCounter++;

            // Add delta to BlipBuffer at the current clock cycle position
            // Scale to match GreenPsgProvider's BlipBuffer levels:
            // Green uses volume(0-64) * masterVolume(204) = max 13056 per channel
            // rawSample has per-channel range 0.0-1.0, so multiply by 13056
            int intSample = (int) (rawSample * 13056);
            int delta = intSample - lastSample;
            if (delta != 0) {
                int time = currentCycle;
                if (time < 0) time = 0;
                buffer.center().addDelta(time, delta);
                lastSample = intSample;
            }

            if (sampleCounter == NUKED_PSG_SAMPLING_HZ) {
                sampleCounter = 0;
                DspUtil.fastHpfResample(rawBuffer, resampleBuffer);
                DspUtil.scale8bit(resampleBuffer, nukedBuffer);
//                writeRawData(rawBuffer);
                if (comparator != null) {
                    comparator.accept(nukedBuffer);
                }
                secondsElapsed++;
            }
        }
//        return hasSample;
    }

    @Override
    public long toPsgCycles(long vgmDelayCycles) {
        return (long) ((vgmDelayCycles * 1.0 / VGM_SAMPLE_RATE_HZ) * CLOCK_HZ);
    }

    final Path rawFile = Paths.get(".", "NUKED_RAW_" + System.currentTimeMillis() + ".raw");

    private void writeRawData(double[] rawBuffer) {
        List<String> l = Arrays.stream(rawBuffer).mapToObj(Double::toString).collect(Collectors.toList());
        Util.writeToFile(rawFile, l);
    }
}
