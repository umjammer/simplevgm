/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.green;

import libgme.util.BlipBuffer;
import libgme.util.StereoBuffer;
import uk.co.omgdrv.simplevgm.model.VgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.psg.gear.PsgProvider;


/**
 * GreenPsgProvider.
 *
 * @author Federico Berti
 * @version 2019
 */
public class GreenPsgProvider extends BaseVgmPsgProvider {

    private static final double NANOS_PER_CYCLE = NANOS_TO_SEC / PsgProvider.GEAR_CLOCK_HZ / 2;
    static final int psgTimeBits = 12;
    static final int psgTimeUnit = 1 << psgTimeBits;
    static final int psgFactor = (int) (1.0 * psgTimeUnit / VGM_SAMPLE_RATE_HZ * CLOCK_HZ + 0.5);

    private final VgmPsgProvider psg;
    private double nanosToNextSample = NANOS_PER_SAMPLE;
    public int sampleCounter = 0;

    public final byte[] greenBuffer = new byte[VGM_SAMPLE_RATE_HZ];

    protected PsgCompare psgCompare;
    protected final PsgCompare.PsgType type = PsgCompare.PsgType.GREEN;
    protected StereoBuffer stereoBuffer;

    public GreenPsgProvider() {
        psg = SmsApu.getInstance();
    }

    public static GreenPsgProvider createInstance(PsgCompare compare) {
        GreenPsgProvider g = (GreenPsgProvider) VgmPsgProvider.getProvider(GreenPsgProvider.class.getName());
        g.psgCompare = compare;
        g.stereoBuffer = new StereoBuffer();
//        g.stereoBuffer.setObserver(new BlipHelper("GreenPsg", false));
        g.stereoBuffer.setSampleRate(VGM_SAMPLE_RATE_HZ, 1000);
        g.stereoBuffer.setClockRate(CLOCK_HZ);
        g.psg.setOutput(g.stereoBuffer.center(), g.stereoBuffer.left(), g.stereoBuffer.right());
        return g;
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
                if (psgCompare != null) {
                    psgCompare.pushData(type, greenBuffer);
                }
            }
        }
    }

    @Override
    protected long toPsgCycles(long vgmDelayCycles) {
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
