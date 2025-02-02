/*
 * Copyright (C) 2003-2007 Shay Green.
 *
 * This module is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This module is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this module; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package uk.co.omgdrv.simplevgm.psg.green;

import libgme.util.BlipBuffer;


/**
 * Sega Master System SN76489 PSG sound chip emulator
 *
 * @author Shay Green
 * @see "https://www.slack.net/~ant/"
 */
public class SmsOsc {

    static final int masterVolume = (int) (0.40 * 65536 / 128);

    BlipBuffer output;
    int outputSelect;
    final BlipBuffer[] outputs = new BlipBuffer[4];
    int delay;
    int lastAmp;
    int volume;

    void reset() {
        delay = 0;
        lastAmp = 0;
        volume = 0;
        outputSelect = 3;
        output = outputs[outputSelect];
    }
}
