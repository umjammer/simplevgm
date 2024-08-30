package uk.co.omgdrv.simplevgm.psg.nuked;

/**
 * PsgYm7101.
 *
 * @author Federico Berti
 * @version Copyright 2019
 */
public interface PsgYm7101 {

    class PsgContext {

        int latch;
        final int[] volume = new int[4];
        final int[] output = new int[4];
        final int[] freq = new int[4];
        final int[] counter = new int[4];
        int sign;
        int noise_data;
        int noise_reset;
        int noise_update;
        int noise_type;
        int noise;
        int inverse;
        int cycle;
        int debug;
    }

    void PSG_Reset(PsgContext context);

    void PSG_Write(PsgContext context, int data);

    int PSG_Read(PsgContext context);

    void PSG_SetDebugBits(PsgContext context, int data);

    double PSG_GetSample(PsgContext context);

    void PSG_Cycle(PsgContext context);
}
