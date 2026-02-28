/*
 * Copyright 2019 Federico Berti
 */

package uk.co.omgdrv.simplevgm.psg.gear2;

import uk.co.omgdrv.simplevgm.psg.gear.GearPsgProvider;
import uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider;


/**
 * Gear2PsgProvider.
 *
 * @author Federico Berti
 * @version 2019
 */
public class Gear2PsgProvider extends GearPsgProvider {

    public Gear2PsgProvider() {
        this.psg = new SN76489Psg();
        this.psg.init(NukedPsgProvider.CLOCK_HZ, VGM_SAMPLE_RATE_HZ);
    }
}