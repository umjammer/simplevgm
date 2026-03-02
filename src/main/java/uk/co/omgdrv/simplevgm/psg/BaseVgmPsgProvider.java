/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg;

import java.util.function.Consumer;

import libgme.util.StereoBuffer;
import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;


/**
 * BaseVgmPsgProvider.
 *
 * @author Federico Berti
 * @version 2019
 */
public abstract class BaseVgmPsgProvider implements VgmPsgProvider {

    public static final double NANOS_TO_SEC = 1_000_000_000;
    public static final int VGM_SAMPLE_RATE_HZ = 44100;
    public static final double NANOS_PER_SAMPLE = NANOS_TO_SEC / VGM_SAMPLE_RATE_HZ;
    public static final int CLOCK_HZ = 3579545;

    protected int currentVgmDelayCycle;
    protected double interpolatedVgmCycle;

    protected StereoBuffer buffer;

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        runUntil(vgmDelayCycles);
    }

    @Override
    public void setOutput(StereoBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void reset() {
        currentVgmDelayCycle = 0;
    }

    @Override
    public void writeGG(int time, int data) {
    }

    @Override
    public void endFrame(int vgmDelayCycles) {
        if (vgmDelayCycles > currentVgmDelayCycle) {
            runUntil(vgmDelayCycles);
        }
        currentVgmDelayCycle -= vgmDelayCycles;
    }

    protected void runUntil(int vgmDelayCycles) {
        if (vgmDelayCycles > currentVgmDelayCycle) {
            long startPsgCycles = toPsgCycles(currentVgmDelayCycle);
            long endPsgCycles = toPsgCycles(vgmDelayCycles);
            long cycles = endPsgCycles - startPsgCycles;
            for (long i = 0; i < cycles; i++) {
                interpolatedVgmCycle = currentVgmDelayCycle + (i * (double) (vgmDelayCycles - currentVgmDelayCycle)) / cycles;
                updateSampleBuffer();
            }
            currentVgmDelayCycle = vgmDelayCycles;
        }
    }

    protected abstract void updateSampleBuffer();

    @Override
    public abstract long toPsgCycles(long vgmDelayCycles);

    // comparator

    protected Consumer<byte[]> comparator;

    public void addComparator(Consumer<byte[]> comparator) {
        this.comparator = comparator;
    }
}
