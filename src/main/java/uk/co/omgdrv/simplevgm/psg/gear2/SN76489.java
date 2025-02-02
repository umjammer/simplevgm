/*
    This file is part of JavaGear.

    Copyright (c) 2002-2008 Chris White
    All rights reserved.

    Redistribution and use of this code or any derivative works are permitted
    provided that the following conditions are met:

    * Redistributions may not be sold, nor may they be used in a commercial
    product or activity.

    * Redistributions that are modified from the original source must include the
    complete source code, including the source code for all components used by a
    binary built from the modified sources. However, as a special exception, the
    source code distributed need not include anything that is normally distributed
    (in either source or binary form) with the major components (compiler, kernel,
    and so on) of the operating system on which the executable runs, unless that
    component itself accompanies the executable.

    * Redistributions must reproduce the above copyright notice, this list of
    conditions and the following disclaimer in the documentation and/or other
    materials provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/

package uk.co.omgdrv.simplevgm.psg.gear2;


/**
 * SN76489 PSG
 *
 * <p>
 * Special Thanks:
 * <ul>
 * <li>This sound rewrite is heavily based on the documentation and research of Maxim.
 * Used and relicensed with permission :)</li>
 * </ul>
 * <p>
 * Timing Notes:
 * <ul>
 *  <li>NTSC Clockspeed = 3579545 Hz</li>
 *  <li>Sample Rate = 44100 Hz</li>
 *  <li>PSG Clock = 223721.5625 Hz (Divide Clockspeed by 16)</li>
 *  <li>Include Sampling Rate = 5.07 (Divide PSG Clock by Sample Rate)</li>
 *  <li>So we want to decrement our counters by 5.07 per cycle</li>
 * </ul>
 * Notes:
 * <ul>
 * <li>To use with other systems other than Sega Master System / GameGear, update the feedback
 * pattern appropriately.</li>
 * </ul>
 *
 * @author Chris White (pointblnk@hotmail.com)
 * @version 17th June 2008
 * @see "http://www.smspower.org/dev/docs/wiki/Sound/PSG"
 */
public class SN76489 {

    /**
     * Fixed point scaling
     */
    private final static int SCALE = 8;

    /**
     * SN76489 Internal Clock Speed (Hz) [SCALED]
     */
    private int clock;

    /**
     * Stores fractional part of clock for various precise updates [SCALED]
     */
    private int clockFrac;

    /**
     * Value to denote that antialiasing should not be used on sample
     */
    private final static int NO_ANTIALIAS = Integer.MIN_VALUE;

    // ----
    // The SN76489 has 8 "registers": 
    // 4 x 4 bit volume registers, 
    // 3 x 10 bit tone registers and 
    // 1 x 3 bit noise register. 
    // ----

    /**
     * SN76489 Registers
     */
    private final int[] reg;

    /**
     * Register Latch
     */
    private int regLatch;

    /**
     * Channel Counters (10-bits on original hardware)
     */
    private final int[] freqCounter;

    /**
     * Polarity of Tone Channel Counters
     */
    private final int[] freqPolarity;

    /**
     * Position of Tone Amplitude Changes
     */
    private final int[] freqPos;

    /**
     * Noise Generator Frequency
     */
    private int noiseFreq;

    /**
     * The Linear Feedback Shift Register (16-bits on original hardware)
     */
    private int noiseShiftReg;

    /**
     * Shift register reset value. Only the highest bit is set
     */
    private final static int SHIFT_RESET = 0x8000;

    /**
     * SMS Only: Tapped bits are bits 0 and 3 ($0009), fed back into bit 15
     */
    private final static int FEEDBACK_PATTERN = 0x9;

    //
    // Output & Amplification
    //

    /**
     * Output channels
     */
    private final int[] outputChannel;

    // Tests with an SMS and a TV card found the highest three volume levels to be clipped    
    private final static int[] PSG_VOLUME = {
            //1516,1205,957,760,603,479,381,303,240,191,152,120,96,76,60,0
            25, 20, 16, 13, 10, 8, 6, 5, 4, 3, 3, 2, 2, 1, 1, 0
    };

    /**
     * SN76489 Constructor.
     */
    public SN76489() {
        // Create various arrays
        outputChannel = new int[4];
        reg = new int[8];
        freqCounter = new int[4];
        freqPolarity = new int[4];
        freqPos = new int[3];
    }

    /**
     * Init SN76496 to Default Values.
     *
     * @param clockSpeed Clock Speed (Hz)
     * @param sampleRate Sample Rate (Hz)
     */
    public void init(int clockSpeed, int sampleRate) {
        // Master clock divided by 16 to get internal clock
        // e.g. 3579545 / 16 / 44100 = 5
        clock = (clockSpeed << SCALE) / 16 / sampleRate;

        regLatch = 0;
        clockFrac = 0;
        noiseShiftReg = SHIFT_RESET;
        noiseFreq = 0x10;

        for (int i = 0; i < 4; i++) {
            // Set Tone Frequency (Don't want this to be zero)
            reg[i << 1] = 1;

            // Set Volume Off
            reg[(i << 1) + 1] = 0x0F;

            // Set Frequency Counters
            freqCounter[i] = 0;

            // Set Amplitudes Positive
            freqPolarity[i] = 1;

            // Do not use intermediate positions
            if (i != 3) freqPos[i] = NO_ANTIALIAS;
        }
    }

    /**
     * Program the SN76489.
     *
     * @param value Value to write (0-0xFF)
     */
    public final void write(int value) {
        //
        // If bit 7 is 1 then the byte is a LATCH/DATA byte.
        //    %1cctdddd
        //    |||````-- Data
        //    ||`------ Type
        //    ``------- Channel
        //

        if ((value & 0x80) != 0) {
            // Bits 6 and 5 ("cc") give the channel to be latched, ALWAYS.
            // Bit 4 ("t") determines whether to latch volume (1) or tone/noise (0) data - 
            // this gives the column.  

            regLatch = (value >> 4) & 7;

            // Zero lower 4 bits of register and mask new value
            reg[regLatch] = (reg[regLatch] & 0x3F0) | (value & 0x0F);
        }

        //
        // If bit 7 is 0 then the byte is a DATA byte.
        //    %0-DDDDDD
        //    |``````-- Data
        //    `-------- Unused
        //

        else {
            // TONE REGISTERS
            // If the currently latched register is a tone register then the low 6
            // bits of the byte are placed into the high 6 bits of the latched register.
            if (regLatch == 0 || regLatch == 2 || regLatch == 4) {
                // ddddDDDDDD (10 bits total) - keep lower 4 bits and replace upper 6 bits.
                // ddddDDDDDD gives the 10-bit half-wave counter reset value.
                reg[regLatch] = (reg[regLatch] & 0x0F) | ((value & 0x3F) << 4);
            }
            // VOLUME & NOISE REGISTERS
            else {
                reg[regLatch] = value & 0x0F;
            }
        }

        switch (regLatch) {
            //
            // Tone register updated
            // If the register value is zero then the output is a constant value of +1.
            // This is often used for sample playback on the SN76489.
            //
            case 0:
            case 2:
            case 4:
                if (reg[regLatch] == 0)
                    reg[regLatch] = 1;
                break;

            //
            // Noise generator updated
            //
            // Noise register:      dddd(DDDDDD) = -trr(---trr)
            //
            // The low 2 bits of dddd select the shift rate and the next highest bit (bit 2) 
            // selects  the mode (white (1) or "periodic" (0)).
            // If a data byte is written, its low 3 bits update the shift rate and mode in the 
            // same way.
            //
            case 6:
                noiseFreq = 0x10 << (reg[6] & 3);
                noiseShiftReg = SHIFT_RESET;
                break;
        }
    }

    public final void update(byte[] buffer, int offset, int samplesToGenerate) {
        for (int sample = 0; sample < samplesToGenerate; sample++) {
            //
            // Generate Sound from Tone Channels
            //
            for (int i = 0; i < 3; i++) {
                if (freqPos[i] != NO_ANTIALIAS)
                    outputChannel[i] = (PSG_VOLUME[reg[(i << 1) + 1]] * freqPos[i]) >> SCALE;
                else
                    outputChannel[i] = PSG_VOLUME[reg[(i << 1) + 1]] * freqPolarity[i];
            }

            //
            // Generate Sound from Noise Channel
            //

            outputChannel[3] = PSG_VOLUME[reg[7]] * (noiseShiftReg & 1) << 1; // Double output

            //
            // Output sound to buffer
            //

            int output = outputChannel[0] + outputChannel[1] + outputChannel[2] + outputChannel[3];

            // Check boundaries
            if (output > 0x7F) output = 0x7F;
            else if (output < -0x80) output = -0x80;

            buffer[offset + sample] = (byte) output;

            //
            // Update Clock
            //
            clockFrac += clock;

            // Contains Main Integer Part (For General Counter Decrements)
            //int clockCyclesPerUpdate = clockFrac &~ ((1 << SCALE) - 1);
            int clockCycles = clockFrac >> SCALE;
            int clockCyclesScaled = clockCycles << SCALE;

            // Clock Counter Updated with Fractional Part Only (For Accurate Stuff Later)
            clockFrac -= clockCyclesScaled;

            //
            // Decrement Counters
            //

            // Decrement Tone Counters
            freqCounter[0] -= clockCycles;
            freqCounter[1] -= clockCycles;
            freqCounter[2] -= clockCycles;

            // Decrement Noise Counter OR Match to Tone 2
            if (noiseFreq == 0x80)
                freqCounter[3] = freqCounter[2];
            else
                freqCounter[3] -= clockCycles;

            //
            // Update 3 x Tone Generators
            //
            for (int i = 0; i < 3; i++) {
                int counter = freqCounter[i];

                // The counter is reset to the value currently in the corresponding register 
                // (eg. Tone0 for channel 0). 
                // The polarity of the output is changed, 
                // ie. if it is currently outputting -1 then it outputs +1, and vice versa.
                if (counter <= 0) {
                    int tone = reg[i << 1];

                    // In tests on an SMS2, the highest note that gave any audible output was 
                    // register value $006, giving frequency 18643Hz (MIDI note A12 -12 cents). 
                    if (tone > 6) {
                        // Calculate what fraction of the way through the sample the flip-flop 
                        // changes state and render it as that fraction of the way through the transition.

                        // Note we divide a scaled number by a scaled number here
                        // So to maintain accuracy we shift the top part of the fraction again
                        freqPos[i] = ((clockCyclesScaled - clockFrac + (2 << SCALE) * counter) << SCALE) *
                                freqPolarity[i] / (clockCyclesScaled + clockFrac);

                        // Flip Polarity
                        freqPolarity[i] = -freqPolarity[i];
                    } else {
                        freqPolarity[i] = 1;
                        freqPos[i] = NO_ANTIALIAS;
                    }

                    // Reset to 10-bit value in corresponding tone register
                    freqCounter[i] += tone * (clockCycles / tone + 1);
                } else {
                    freqPos[i] = NO_ANTIALIAS;
                }
            }

            //
            // Update Noise Generators
            //
            if (freqCounter[3] <= 0) {
                // Flip Polarity
                freqPolarity[3] = -freqPolarity[3];

                // Not matching Tone 2 Value, so reload counter
                if (noiseFreq != 0x80)
                    freqCounter[3] += noiseFreq * (clockCycles / noiseFreq + 1);

                // Positive Amplitude i.e. We only want to do this once per cycle
                if (freqPolarity[3] == 1) {
                    int feedback;

                    // White Noise Selected
                    if ((reg[6] & 0x04) != 0) {
                        // If two bits fed back, I can do Feedback=(nsr & fb) && (nsr & fb ^ fb)
                        // since that's (one or more bits set) && (not all bits set)
                        feedback = (noiseShiftReg & FEEDBACK_PATTERN) != 0 &&
                                ((noiseShiftReg & FEEDBACK_PATTERN) ^ FEEDBACK_PATTERN) != 0
                                ? 1 : 0;
                    }
                    // Periodic Noise Selected
                    else {
                        feedback = noiseShiftReg & 1;
                    }

                    noiseShiftReg = (noiseShiftReg >> 1) | (feedback << 15);
                }
            }
        }
    }
}
