/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.green;

import libgme.util.BlipBuffer;
import libgme.util.StereoBuffer;
import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.gear.GearPsg;


/**
 * GreenPsgProvider.
 *
 * @author Federico Berti
 * @version 2019
 */
public class GreenPsgProvider extends BaseVgmPsgProvider {

    private static final double NANOS_PER_CYCLE = NANOS_TO_SEC / GearPsg.GEAR_CLOCK_HZ / 2;
    static final int psgTimeBits = 12;
    static final int psgTimeUnit = 1 << psgTimeBits;
    static final int psgFactor = (int) (1.0 * psgTimeUnit / VGM_SAMPLE_RATE_HZ * CLOCK_HZ + 0.5);

    private final SmsApu psg = new SmsApu();
    private double nanosToNextSample = NANOS_PER_SAMPLE;
    public int sampleCounter = 0;

    public final byte[] greenBuffer = new byte[VGM_SAMPLE_RATE_HZ];

    protected StereoBuffer stereoBuffer;

    public GreenPsgProvider() {
        // TODO copy from compare instantiation
        this.stereoBuffer = new StereoBuffer();
//        this.stereoBuffer.setObserver(new BlipHelper("GreenPsg", false));
        this.stereoBuffer.setSampleRate(VGM_SAMPLE_RATE_HZ, 1000);
        this.stereoBuffer.setClockRate(GreenPsgProvider.CLOCK_HZ);
        this.psg.setOutput(this.stereoBuffer.center(), this.stereoBuffer.left(), this.stereoBuffer.right());
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
//        super.writeData(vgmDelayCycles, data);
//        psg.writeData((int) toPsgCycles(vgmDelayCycles), data);
        psg.writeData(vgmDelayCycles, data);
    }

    private void endFrameInternal(int vgmDelayCycles) {
        int time = (int) toPsgCycles(vgmDelayCycles);
        psg.endFrame(time);
        stereoBuffer.endFrame(vgmDelayCycles);
    }

    @Override
    protected void updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
        if (nanosToNextSample < 0) {
            nanosToNextSample += NANOS_PER_SAMPLE;
            sampleCounter++;
            if (sampleCounter == VGM_SAMPLE_RATE_HZ) {
                endFrameInternal(VGM_SAMPLE_RATE_HZ);
                int read = stereoBuffer.readSamples(greenBuffer, 0, greenBuffer.length);
                sampleCounter = 0;
                if (comparator != null) {
                    comparator.accept(greenBuffer);
                }
            }
        }
    }

    @Override
    public long toPsgCycles(long vgmDelayCycles) {
        return (vgmDelayCycles * psgFactor + psgTimeUnit / 2) >> psgTimeBits;
    }

    @Override
    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {
        psg.setOutput(center, left, right);
    }

    @Override
    public void reset() {
        psg.reset();
    }

    @Override
    public void writeGG(int time, int data) {
        psg.writeGG(time, data);
    }

    @Override
    public void endFrame(int vgmDelayCycles) {
        psg.endFrame(vgmDelayCycles);
    }
}
