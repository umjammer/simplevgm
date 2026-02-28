/*
 * Copyright (C) 2002-2003 Chris White
 *
 * This file is part of JavaGear.
 *
 * JavaGear is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JavaGear is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaGear; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package uk.co.omgdrv.simplevgm.psg.gear;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;

import static java.lang.System.getLogger;


/**
 * Texas SN76496 Emulation.
 *
 * @author Chris White
 * @version 18th January 2003
 * @see "JavaGear Final Project Report"
 */
public final class SN76496Psg implements GearPsg {

    private static final Logger logger = getLogger(SN76496Psg.class.getName());

    /** the chip */
    private SN76496 psg;

    /**
     * Reset SN76496 to Default Values.
     */
    @Override
    public void reset() {
        psg.reset();
    }

    @Override
    public void init(int clockSpeed, int sampleRate) {
        psg = new SN76496(clockSpeed, sampleRate);

        //AudioFormat(sample_rate(hz), sampleSizeInBits, channels, signed, bigEndian)
        audioFormat = new AudioFormat(sampleRate, PSG_OUTPUT_SAMPLE_SIZE, PSG_OUTPUT_CHANNELS, true, false);
    }

    /**
     * Program the PSG. Connected this procedure to a Z80 Port.
     *
     * @param value Value to write (0-0xFF)
     */
    @Override
    public void write(int value) {
        if (!psg.isEnabled() && !isRecording()) {
            return;
        }

        psg.write(value);
    }

    @Override
    public void output(byte[] buffer, int offset, int end) {
        if (!psg.isEnabled() && !isRecording()) {
            return;
        }

        psg.output(buffer, offset, end);

        // Loop for length of this video frame
        for (int i = offset; i < end; i++) {
            if (isRecording()) {
                try {
                    fileWriter.write(buffer[i] & 0xff); // output 8 bit signed mono
                } catch (IOException ioe) {
                    logger.log(Level.ERROR, "An error occurred while writing the sound file.");
                }
            }
        }
    }

    /**
     * Convert PSG settings to Java Sound.
     */
    @Override
    public void output(byte[] buffer) {
        output(buffer, 0, buffer.length);
    }

//#region RECORDING

    /**
     * Record Sound to Disk.
     */
    private boolean recording;

    private AudioFormat audioFormat;

    /**
     * For Recording Sound to Disk.
     */
    private FileWriter fileWriter;

    /**
     * Toggle sound recording to WAV file.
     */
    public void setRecord() {
        if (isRecording()) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    /**
     * Start sound recording to WAV file.
     */
    private void startRecording() {
        if (!isRecording()) {
            try {
                fileWriter = new FileWriter("output.raw");
            } catch (IOException ioe) {
                logger.log(Level.ERROR, "Could not open file for recording.");
            }
            setRecording(true);
        }
    }

    /**
     * Stop sound recording to WAV file.
     */
    public void stopRecording() {
        if (isRecording()) {
            try {
                fileWriter.close();
                convertToWav();
            } catch (IOException ioe) {
                logger.log(Level.ERROR, "Failed whilst closing output.raw");
            }
            setRecording(false);
        }
    }

    public boolean isRecording() {
        return recording;
    }

    private void setRecording(boolean recording) {
        this.recording = recording;
    }

    /**
     * Convert RAW output to WAV file.
     */
    private void convertToWav() {
//        SoundUtil.convertToWav(audioFormat, "ouput.raw");
    }

    public static void main(String[] args) {
        AudioFormat audioFormat = new AudioFormat(11025, 8, 1, true, false);
//        SoundUtil.convertToWav(audioFormat, "ouput.raw");
    }

//#endregion
}
