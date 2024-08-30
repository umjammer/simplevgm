package uk.co.omgdrv.simplevgm.util;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import libgme.util.StereoBuffer;

import static java.lang.System.getLogger;


/**
 * BlipHelper.
 *
 * @author Federico Berti
 * @version Copyright 2019
 */
public class BlipHelper implements StereoBuffer.Observer {

    private static final Logger logger = getLogger(BlipHelper.class.getName());

    static final Path file = Paths.get(".", System.currentTimeMillis() + ".raw");
    static final boolean writeToFile = true;

    String name;
    boolean bit8;

    public BlipHelper(String name, boolean bit8) {
        this.name = name;
        this.bit8 = bit8;
    }

    /**
     * out[0] = MSB LEFT
     * out[1] = LSB RIGHT
     * out[2] = MSB RIGHT
     * out[3] = LSB RIGHT
     */
    @Override
    public void observe(byte[] out, int start, int end) {
        List<String> lines = new ArrayList<>();
        for (int i = start; i < end; i += 4) {
            int left = Util.getSigned16BE(out[i], out[i + 1]);
            int right = Util.getSigned16BE(out[i + 2], out[i + 3]);
            int val = (left + right) >> 1;
            int printVal = bit8 ? val >> 8 : val;
            System.out.println(name + "," + printVal);
            lines.add("" + val);
        }
        writeToFile(lines);
    }

    private static void writeToFile(List<String> lines) {
        if (writeToFile) {
            try {
                Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }
    }
}
