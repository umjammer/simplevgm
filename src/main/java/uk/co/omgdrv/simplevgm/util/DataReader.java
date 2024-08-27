package uk.co.omgdrv.simplevgm.util;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;


/**
 * @see "https://www.slack.net/~ant/"
 */
public class DataReader {

    static InputStream openGZIP(InputStream in) throws Exception {
        return new GZIPInputStream(in);
    }

    // Loads entire stream into byte array
    static byte[] loadData(InputStream in) throws Exception {
        byte[] data = new byte[256 * 1024];
        int size = 0;
        int count;
        while ((count = in.read(data, size, data.length - size)) != -1) {
            size += count;
            if (size >= data.length)
                data = Util.resize(data, data.length * 2);
        }

        if (data.length - size > data.length / 4)
            data = Util.resize(data, size);

        return data;
    }
}
