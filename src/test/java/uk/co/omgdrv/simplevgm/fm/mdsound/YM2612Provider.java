/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package uk.co.omgdrv.simplevgm.fm.mdsound;

import java.lang.System.Logger.Level;

import mdsound.chips.Ym2612;
import uk.co.omgdrv.simplevgm.model.VgmFmProvider;


/**
 * MDSound's YM2612 Provider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-01 nsano initial version <br>
 */
public class YM2612Provider implements VgmFmProvider {

    private Ym2612 ym2612 = new Ym2612();

    @Override
    public void reset() {
if (ym2612 == null) {
 logger.log(Level.INFO, "not #init(), " + this);
 return;
}
        ym2612.reset();
    }

    @Override
    public void init(int clock, int rate) {
logger.log(Level.INFO, "init: clock: " + clock + ", rate: " + rate + ", " + this);
        ym2612.init(clock, rate, 0);
    }

    @Override
    public void update(int[] buf_lr, int offset, int end) {
//Debug.println("ofs: " + offset + ", end: " + end + ", buf: " + buf_lr.length);
        int L = Ym2612.MAX_UPDATE_LENGTH;
        int y = (end + (L - 1)) / L;
        for (int x = 0; x < y; x++) {
            int z = x < y - 1 ? L : end % L;
            int[][] buf = new int[2][z];
            ym2612.update(buf, z);
            for (int i = 0; i < z; i++) {
                buf_lr[offset * 2 + i * 2 + 0] = buf[0][i];
                buf_lr[offset * 2 + i * 2 + 1] = buf[1][i];
            }
            offset += z;
        }
    }

    @Override
    public int readRegister(int type, int regNumber) {
        return ym2612.getRegisters()[type][regNumber];
    }

    @Override
    public void writePort(int addr, int data) {
        ym2612.write(addr, data);
    }

    @Override
    public int read() {
        return ym2612.read();
    }
}
