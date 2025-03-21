package uk.co.omgdrv.simplevgm.util;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.System.getLogger;


/**
 * DspUtil.
 *
 * @author Federico Berti
 * @version Copyright 2019
 */
public class DspUtil {

    private static final Logger logger = getLogger(DspUtil.class.getName());

    public static final int PSG_MAX_VOLUME_8_BIT = 0x20;
    public static final int PSG_MAX_VOLUME_16_BIT = 0x7FFF;

    public static void fastHpfResample(double[] in, double[] out) {
        int sampleRatio = in.length / out.length;
        int sampleRatioHalf = sampleRatio >> 1;
        double totalSamplesWithRatio = in.length / sampleRatio;
        int extraSamplePos = (int) Math.ceil(1 / ((totalSamplesWithRatio / out.length) - 1));
        int k = 1, l = 0;
        for (int i = sampleRatio; i < in.length - 1; i += sampleRatio) {
            l++;
            if (l % extraSamplePos == 0) {
                continue;
            }
            double sample = (in[i - sampleRatioHalf] + in[i + sampleRatioHalf]) / 2; // resample
            out[k] = sample - out[k - 1] / 2; // hpf
            k++;
        }
        // NOTE: no hpf here
        for (int i = k; i < out.length; i++) {
            out[i] = out[k - 1];
        }
    }

    public static void fastHpfResample(int[] in, int[] out) {
        int sampleRatio = in.length / out.length;
        int sampleRatioHalf = sampleRatio >> 1;
        double totalSamplesWithRatio = in.length / sampleRatio;
        int extraSamplePos = (int) Math.ceil(1 / ((totalSamplesWithRatio / out.length) - 1));
        int k = 0, l = 0;
        for (int i = sampleRatio; i < in.length - 1; i += sampleRatio) {
            l++;
            if (l % extraSamplePos == 0) {
                continue;
            }
            int sample = (in[i - sampleRatioHalf] + in[i + sampleRatioHalf]) >> 1; // resample
            out[k] = sample - (out[k - 1] >> 1); // hpf
            k++;
        }
        // NOTE: no hpf here
        for (int i = k; i < out.length; i++) {
            out[i] = out[k - 1];
        }
    }

    public static void resample(double[] in, double[] out) {
        int sampleRatio = in.length / out.length;
        int sampleRatioHalf = sampleRatio >> 1;
        double totalSamplesWithRatio = in.length / sampleRatio;
        int extraSamplePos = (int) Math.ceil(1 / ((totalSamplesWithRatio / out.length) - 1));
        int k = 0, l = 0;
        for (int i = sampleRatio; i < in.length - 1; i += sampleRatio) {
            l++;
            if (l % extraSamplePos == 0) {
                continue;
            }
            out[k++] = (in[i - sampleRatioHalf] + in[i + sampleRatioHalf]) / 2;
        }
        // NOTE: no hpf here
        for (int i = k; i < out.length; i++) {
            out[i] = out[k - 1];
        }
    }

    /**
     * HighPass FIR filter with zero at 0hz
     * Y[n] = X[n] - 0.5*X[n-1]
     */
    public static void fastHpf(double[] in, double[] out) {
        for (int i = 1; i < in.length; i++) {
            out[i] = in[i] - in[i - 2] / 2;
        }
        out[0] = in[0];
    }

    public static void scale8bit(double[] in, byte[] out) {
        for (int i = 0; i < in.length; i++) {
            out[i] = scaleClamp8bit(in[i], PSG_MAX_VOLUME_8_BIT);
        }
    }

    public static byte scaleClamp8bit(double val, int volumeFactor) {
        int res = (int) Math.round(val * volumeFactor);
        return res > Byte.MAX_VALUE ? Byte.MAX_VALUE : (byte) (res < Byte.MIN_VALUE ? Byte.MIN_VALUE : res);
    }

    public static short scaleClamp16bit(double val, int volumeFactor) {
        int res = (int) Math.round(val * volumeFactor);
        return res > Short.MAX_VALUE ? Short.MAX_VALUE : (short) (res < Short.MIN_VALUE ? Short.MIN_VALUE : res);
    }

    public static void printByte(String name, byte[] in) {
        printStream(IntStream.range(0, in.length).mapToObj(i -> (name + "," + in[i])));
    }

    public static void printDouble(String name, double[] in) {
        printStream(Arrays.stream(in).mapToObj(d -> (name + "," + d)));
    }

    public static void printFloat(String name, float[] in) {
        printStream(IntStream.range(0, in.length).mapToObj(i -> (name + "," + in[i])));
    }

    public static void printInt(String name, int[] in) {
        printStream(Arrays.stream(in).mapToObj(d -> (name + "," + d)));
    }

    private static void printStream(Stream<String> s) {
        logger.log(Level.DEBUG, s.collect(Collectors.joining("\n")));
    }
}
