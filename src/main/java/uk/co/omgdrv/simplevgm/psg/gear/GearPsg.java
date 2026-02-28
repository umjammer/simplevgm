/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.gear;

import uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider;


/**
 * PsgProvider.
 *
 * @author Federico Berti
 * @version 2018
 */
public interface GearPsg {

    int GEAR_CLOCK_HZ = NukedPsgProvider.CLOCK_HZ / 32;

    int PSG_OUTPUT_SAMPLE_SIZE = 8;
    int PSG_OUTPUT_CHANNELS = 1;

    /**
     * @param clockSpeed Clock Speed (Hz)
     * @param sampleRate Sample Rate (Hz)
     */
    void init(int clockSpeed, int sampleRate);

    void write(int data);

    void output(byte[] output);

    void output(byte[] output, int offset, int end);

    void reset();

    GearPsg NO_SOUND = new GearPsg() {

        @Override public void init(int clockSpeed, int sampleRate) {
        }

        @Override public void write(int data) {
        }

        @Override public void output(byte[] ouput) {
        }

        @Override public void output(byte[] output, int offset, int end) {
        }

        @Override public void reset() {
        }
    };
}
