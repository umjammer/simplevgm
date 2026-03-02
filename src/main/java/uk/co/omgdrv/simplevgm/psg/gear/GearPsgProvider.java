/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.gear;

import java.lang.System.Logger;

import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;


/**
 * GearPsgProvider.
 *
 * @author Federico Berti
 * @version 2019
 */
public class GearPsgProvider extends BaseVgmPsgProvider {

    private static final Logger logger = System.getLogger(GearPsgProvider.class.getName());

    // For toPsgCycles - convert VGM samples (44100 Hz) to PSG clock cycles (3579545 Hz)
    // Same formula as GreenPsgProvider, matching the BlipBuffer clock domain
    static final int psgTimeBits = 12;
    static final int psgTimeUnit = 1 << psgTimeBits;
    static final int psgFactor = (int) (1.0 * psgTimeUnit / VGM_SAMPLE_RATE_HZ * CLOCK_HZ + 0.5);

    // Clock cycles per SN76496 output sample: CLOCK_HZ / VGM_SAMPLE_RATE_HZ ≈ 81.17
    private static final double CLOCKS_PER_SAMPLE = (double) CLOCK_HZ / VGM_SAMPLE_RATE_HZ;

    protected GearPsg psg;
    /** per-frame sample count, reset at endFrame, used for BlipBuffer delta timing */
    private int frameSampleCount = 0;
    /** accumulating sample index into gearBuffer, wraps at VGM_SAMPLE_RATE_HZ for comparator */
    public int sampleCounter = 0;
    public final byte[] gearBuffer = new byte[VGM_SAMPLE_RATE_HZ + 100000];

    private int lastSample = 0;

    public GearPsgProvider() {
        this.psg = new SN76496Psg();
        this.psg.init(GearPsg.GEAR_CLOCK_HZ, VGM_SAMPLE_RATE_HZ);
    }

    @Override
    public void writeData(int clockTime, int data) {
        // Generate samples up to this clock time, adding deltas to BlipBuffer
        // Do NOT call super.writeData() — that calls runUntil() which double-converts time
        generateSamplesUntil(clockTime);
        psg.write(data);
    }

    @Override
    public void endFrame(int clockEndTime) {
        // Generate remaining samples up to end of frame
        generateSamplesUntil(clockEndTime);
        // Reset per-frame counter for next frame
        frameSampleCount = 0;
    }

    @Override
    public long toPsgCycles(long vgmDelayCycles) {
//logger.log(Level.INFO, "vgmDelayCycles: " + vgmDelayCycles);
        // Convert VGM sample time (44100 Hz) to PSG clock cycles (3579545 Hz)
        // Must return PSG clock domain for BlipBuffer compatibility
        return (vgmDelayCycles * psgFactor + psgTimeUnit / 2) >> psgTimeBits;
    }

    /**
     * Generate SN76496 samples up to the given clock cycle time,
     * converting each sample change to a BlipBuffer delta.
     */
    private void generateSamplesUntil(int clockTime) {
        // How many total per-frame samples should exist by this clock time?
        int targetSamples = (int) (clockTime / CLOCKS_PER_SAMPLE);
        while (frameSampleCount < targetSamples) {
            updateSampleBuffer();
        }
    }

    @Override
    protected void updateSampleBuffer() {
        psg.output(gearBuffer, sampleCounter, sampleCounter + 1);

        int sample = gearBuffer[sampleCounter];
        int intSample = sample * 64;
        int delta = intSample - lastSample;
        if (delta != 0) {
            // Map this sample's position to PSG clock cycles for BlipBuffer
            int time = (int) (frameSampleCount * CLOCKS_PER_SAMPLE);
            if (time < 0) time = 0;
            buffer.center().addDelta(time, delta);
            lastSample = intSample;
        }

        frameSampleCount++;
        sampleCounter++;
        if (sampleCounter == VGM_SAMPLE_RATE_HZ) {
            sampleCounter = 0;
            if (comparator != null) {
                comparator.accept(gearBuffer);
            }
        }
    }
}
