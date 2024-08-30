package uk.co.omgdrv.simplevgm.model;

import libgme.util.BlipBuffer;


/**
 * NullVgmPsgProvider.
 *
 * @author Federico Berti
 * @version Copyright 2019
 */
public class NullVgmPsgProvider implements VgmPsgProvider {

    @Override
    public void writeData(int time, int data) {
    }

    @Override
    public void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right) {
    }

    @Override
    public void reset() {
    }

    @Override
    public void writeGG(int time, int data) {
    }

    @Override
    public void endFrame(int endTime) {
    }
}
