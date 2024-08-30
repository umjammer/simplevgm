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

package uk.co.omgdrv.simplevgm;

import uk.co.omgdrv.simplevgm.util.Util;


/**
 * Music emulator interface
 *
 * @see "https://www.slack.net/~ant"
 */
public abstract class MusicEmu {

    public MusicEmu() {
        trackCount = 0;
        trackEnded = true;
        currentTrack = 0;
    }

    // Requests change of sample rate and returns sample rate used, which might be different
    public final int setSampleRate(int rate) {
        return sampleRate = setSampleRate_(rate);
    }

    public final int sampleRate() {
        return sampleRate;
    }

    // Loads music file into emulator. Might keep reference to data.
    public void loadFile(byte[] data) {
        trackEnded = true;
        currentTrack = 0;
        currentTime = 0;
        trackCount = parseHeader(data);
    }

    // Number of tracks
    public final int trackCount() {
        return trackCount;
    }

    // Starts track, where 0 is first track
    public void startTrack(int track) {
        if (track < 0 || track > trackCount) {
            throw new IllegalArgumentException("Invalid track: " + track);
        }

        trackEnded = false;
        currentTrack = track;
        currentTime = 0;
        fadeStart = 0x4000_0000; // far into the future
        fadeStep = 1;
    }

    // Currently started track
    public final int currentTrack() {
        return currentTrack;
    }

    // Generates at most count samples into out and returns
    // number of samples written. If track has ended, fills
    // buffer with silence.
    public final int play(byte[] out, int count) {
        if (!trackEnded) {
            count = play_(out, count);
            if ((currentTime += count >> 1) > fadeStart)
                applyFade(out, count);
        } else {
            java.util.Arrays.fill(out, 0, count * 2, (byte) 0);
        }
        return count;
    }

    // Sets fade start and length, in seconds. Must be set after call to startTrack().
    public final void setFade(int start, int length) {
        fadeStart = sampleRate * Math.max(0, start);
        fadeStep = sampleRate * length / (fadeBlockSize * fadeShift);
        if (fadeStep < 1)
            fadeStep = 1;
    }

    // Number of seconds current track has been played
    public final int currentTime() {
        return currentTime / sampleRate;
    }

    // True if track has reached end or setFade()'s fade has finished
    public final boolean trackEnded() {
        return trackEnded;
    }

    // protected

    // must be defined in derived class
    protected abstract int setSampleRate_(int rate);

    protected abstract int parseHeader(byte[] in);

    protected abstract int play_(byte[] out, int count);

    // Sets end of track flag and stops emulating file
    protected void setTrackEnded() {
        trackEnded = true;
    }

    // private

    int sampleRate;
    int trackCount;
    int currentTrack;
    int currentTime;
    int fadeStart;
    int fadeStep;
    boolean trackEnded;

    static final int fadeBlockSize = 512;
    static final int fadeShift = 8; // fade ends with gain at 1.0 / (1 << fadeShift)
    static final int gainShift = 14;
    static final int gainUnit = 1 << gainShift;

    /** Scales count big-endian 16-bit samples from io [pos*2] by gain/gainUnit */
    static void scaleSamples(byte[] io, int pos, int count, int gain) {
        pos <<= 1;
        count = (count << 1) + pos;
        do {
            int s;
            io[pos + 1] = (byte) (s = ((io[pos] << 8 | (io[pos + 1] & 0xFF)) * gain) >> gainShift);
            io[pos] = (byte) (s >> 8);
        }
        while ((pos += 2) < count);
    }

    private void applyFade(byte[] io, int count) {
        // Apply successively smaller gains based on time since fade start
        for (int i = 0; i < count; i += fadeBlockSize) {
            // logarithmic progression
            int gain = Util.int_log((currentTime + i - fadeStart) / fadeBlockSize, fadeStep, gainUnit);
            if (gain < (gainUnit >> fadeShift))
                setTrackEnded();

            int n = count - i;
            if (n > fadeBlockSize)
                n = fadeBlockSize;
            scaleSamples(io, i, n, gain);
        }
    }
}
