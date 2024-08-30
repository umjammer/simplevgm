/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package uk.co.omgdrv.simplevgm.model;

/**
 * NullVgmFmProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-08-30 nsano initial version <br>
 */
public class NullVgmFmProvider implements VgmFmProvider {

    @Override
    public void reset() {

    }

    @Override
    public void init(int Clock, int Rate) {

    }

    @Override
    public void update(int[] buf_lr, int offset, int end) {

    }

    @Override
    public void writePort(int addr, int data) {

    }

    @Override
    public void write(int addr, int data) {

    }
}
