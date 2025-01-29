package uk.co.omgdrv.simplevgm.test;

import java.io.IOException;
import java.util.Arrays;

import uk.co.omgdrv.simplevgm.psg.BaseVgmPsgProvider;
import uk.co.omgdrv.simplevgm.psg.nuked.NukedPsgProvider;
import uk.co.omgdrv.simplevgm.util.DspUtil;
import vavi.util.Debug;


/**
 * ${FILE}
 * <p>
 * Federico Berti
 * <p>
 * Copyright 2019
 */
public class FilterPerfTest {

    public static final int WARM_UP = 50;
    public static final int TEST = 1000;

    static final double NANOS_PER_SECOND = 1000_000_000;

    static final double[] outBuffer = new double[BaseVgmPsgProvider.VGM_SAMPLE_RATE_HZ];

    public static void main(String[] args) throws IOException {
        testFastHpfResamplePerf();
    }

    public static void testFastHpfResamplePerf() throws IOException {
        double[] data = FilterTest.getFileContents();
        double[] oneSecData = Arrays.copyOfRange(data, 0, NukedPsgProvider.NUKED_PSG_SAMPLING_HZ);

        do {
            for (int i = 0; i < WARM_UP; i++) {
                DspUtil.fastHpfResample(oneSecData, outBuffer);
            }

            long start = System.nanoTime();
            for (int i = 0; i < TEST; i++) {
                DspUtil.fastHpfResample(oneSecData, outBuffer);
            }
            long durationNs = System.nanoTime() - start;
Debug.println("hpfResample: " + durationNs / NANOS_PER_SECOND + " sec, cycles: " + TEST);
        } while (true);
    }
}
