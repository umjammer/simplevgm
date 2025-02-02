/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.gear2;

import uk.co.omgdrv.simplevgm.psg.gear.PsgProvider;


/**
 * SN76489Psg.
 *
 * @author Federico Berti
 * @version 2019
 */
public class SN76489Psg implements PsgProvider { // TODO gross

    private SN76489 psg;

    public static SN76489Psg createInstance(int clockSpeed, int sampleRate) {
        SN76489Psg s = new SN76489Psg();
        s.psg = new SN76489();
        s.psg.init(clockSpeed, sampleRate);
        return s;
    }

    @Override
    public void init() {
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
