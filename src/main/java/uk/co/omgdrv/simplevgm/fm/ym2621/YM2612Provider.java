/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package uk.co.omgdrv.simplevgm.fm.ym2621;


import uk.co.omgdrv.simplevgm.fm.MdFmProvider;


/**
 * YM2612Provider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-08-30 nsano initial version <br>
 */
public class YM2612Provider implements MdFmProvider {

    private final YM2612 ym2612 = new YM2612();

    @Override
    public void reset() {
        ym2612.reset();
    }

    @Override
    public void init(int clock, int rate) {
        ym2612.init(clock, rate);
    }

    @Override
    public void update(int[] buf_lr, int offset, int end) {
        ym2612.update(buf_lr, offset, end);
    }

    @Override
    public int readRegister(int type, int regNumber) {
        return ym2612.readRegister(type, regNumber);
    }

    @Override
    public void writePort(int addr, int data) {
        ym2612.writePort(addr, data);
    }

    @Override
    public int read() {
        return ym2612.read();
    }
}
