/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.gear;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;


/**
 * GearPsgProvider.
 *
 * @author Federico Berti
 * @version 2019
 */
public class GearPsgProvider extends BaseVgmPsgProvider {

    private static final Logger logger = System.getLogger(GearPsgProvider.class.getName());

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
logger.log(Level.INFO, "vgmDelayCycles: " + vgmDelayCycles);
        return (long) ((vgmDelayCycles * 1.0 / VGM_SAMPLE_RATE_HZ) * GearPsg.GEAR_CLOCK_HZ);
    }

    private int lastSample = 0;

    @Override
    protected void updateSampleBuffer() {
        nanosToNextSample -= NANOS_PER_CYCLE;
        if (nanosToNextSample < 0) {
            nanosToNextSample += NANOS_PER_SAMPLE;
            psg.output(gearBuffer, sampleCounter, sampleCounter + 1);

            int sample = gearBuffer[sampleCounter];
            int intSample = sample * 64;
            int delta = intSample - lastSample;
            if (delta != 0) {
                // Use the perfectly interpolated timeline from BaseVgmPsgProvider
                int time = (int) toPsgCycles((long) interpolatedVgmCycle);
                if (time < 0) time = 0;
                buffer.center().addDelta(time, delta);
                lastSample = intSample;
            }

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
