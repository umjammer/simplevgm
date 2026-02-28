/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.gear2;

import uk.co.omgdrv.simplevgm.psg.gear.GearPsg;


/**
 * SN76489Psg.
 *
 * @author Federico Berti
 * @version 2019
 */
public class SN76489Psg implements GearPsg {

    private SN76489 psg;

    @Override
    public void init(int clockSpeed, int sampleRate) {
        this.psg = new SN76489();
        this.psg.init(clockSpeed, sampleRate);
    }

    @Override
    public void write(int data) {
        psg.write(data);
    }

    @Override
    public void output(byte[] output) {
        psg.update(output, 0, output.length);
    }

    @Override
    public void output(byte[] output, int offset, int end) {
        psg.update(output, offset, end - offset);
    }

    @Override
    public void reset() {
    }
}
