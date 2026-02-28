/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.gear;

import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;


/**
 * GearPsgProvider.
 *
 * @author Federico Berti
 * @version 2019
 */
public class GearPsgProvider extends BaseVgmPsgProvider {

    private static final double NANOS_PER_CYCLE = NANOS_TO_SEC / GearPsg.GEAR_CLOCK_HZ / 2; // div2 TODO Why

    protected GearPsg psg;
    private double nanosToNextSample = NANOS_PER_SAMPLE;
    public int sampleCounter = 0;
    public final byte[] gearBuffer = new byte[VGM_SAMPLE_RATE_HZ];

    public GearPsgProvider() {
        this.psg = new SN76496Psg();
        this.psg.init(GearPsg.GEAR_CLOCK_HZ, VGM_SAMPLE_RATE_HZ);
    }

    @Override
    public void writeData(int vgmDelayCycles, int data) {
        super.writeData(vgmDelayCycles, data);
        psg.write(data);
    }

    @Override
    public long toPsgCycles(long vgmDelayCycles) {
        return (long) ((vgmDelayCycles * 1.0 / VGM_SAMPLE_RATE_HZ) * GearPsg.GEAR_CLOCK_HZ);
    }

    @Override
    protected void updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
        if (nanosToNextSample < 0) {
            nanosToNextSample += NANOS_PER_SAMPLE;
            psg.output(gearBuffer, sampleCounter, sampleCounter + 1);
            sampleCounter++;
            if (sampleCounter == VGM_SAMPLE_RATE_HZ) {
                sampleCounter = 0;
                if (comparator != null) {
                    comparator.accept(gearBuffer);
                }
            }
        }
    }
}
