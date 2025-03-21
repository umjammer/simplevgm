package uk.co.omgdrv.simplevgm.model;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import uk.co.omgdrv.simplevgm.util.Util;

import static java.lang.System.getLogger;
import static uk.co.omgdrv.simplevgm.model.Gd3Tag.Gd3String.AUTHOR_NAME;
import static uk.co.omgdrv.simplevgm.model.Gd3Tag.Gd3String.GAME_NAME;
import static uk.co.omgdrv.simplevgm.model.Gd3Tag.Gd3String.NOTES;
import static uk.co.omgdrv.simplevgm.model.Gd3Tag.Gd3String.RELEASE_DATE;
import static uk.co.omgdrv.simplevgm.model.Gd3Tag.Gd3String.SYSTEM_NAME;
import static uk.co.omgdrv.simplevgm.model.Gd3Tag.Gd3String.TRACK_NAME;
import static uk.co.omgdrv.simplevgm.model.Gd3Tag.Gd3String.VGM_CREATED_BY;


/**
 * Gd3Tag.
 *
 * @author Federico Berti
 * @version Copyright 2019
 */
public class Gd3Tag {

    private static final Logger logger = getLogger(Gd3Tag.class.getName());

    public static final String VGM_GD3_MAGIC_WORD = "Gd3 ";

    public static final Gd3Tag NO_TAG = new Gd3Tag();

    enum Gd3String {
        TRACK_NAME,
        TRACK_NAME_JP,
        GAME_NAME,
        GAME_NAME_JP,
        SYSTEM_NAME,
        SYSTEM_NAME_JP,
        AUTHOR_NAME,
        AUTHOR_NAME_JP,
        RELEASE_DATE,
        VGM_CREATED_BY,
        NOTES
    }

    private final static Gd3String[] toPrint = {
            TRACK_NAME, GAME_NAME, SYSTEM_NAME, AUTHOR_NAME, RELEASE_DATE, VGM_CREATED_BY, NOTES
    };

    private final Map<Gd3String, String> map = new TreeMap<>();
    private int gd3Len;
    private int iver;
    private String version;

    final Function<Gd3String, String> toStr = gd -> map.get(gd).isEmpty() ? "" : gd + ": " + map.get(gd);

    public String toDataString() {
        String sb = "\nGd3Tag v" + version + "\n" +
                Arrays.stream(toPrint).map(toStr).collect(Collectors.joining("\n"));
        return sb;
    }

    public static Gd3Tag parseTag(byte[] data, int position) {
        Gd3Tag g = new Gd3Tag();
        try {
            String tag = new String(data, position, 4);
            if (!VGM_GD3_MAGIC_WORD.equalsIgnoreCase(tag)) {
                return NO_TAG;
            }
            g.iver = Util.getUInt32LE(data, position + 4);
            g.version = VgmHeader.toVersionString(g.iver);
            g.gd3Len = Util.getUInt32LE(data, position + 8);
            parseStrings(g, data, position + 12);
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            g = NO_TAG;
        }
        return g;
    }

    private static void parseStrings(Gd3Tag g, byte[] data, int start) {
        StringBuilder str = new StringBuilder();
        int cnt = 0;
        for (int i = start; i < start + g.gd3Len && cnt < Gd3String.values().length; i += 2) {
            int val = Util.getSigned16LE(data[i], data[i + 1]);
            if (val == 0) {
                Gd3String gs = Gd3String.values()[cnt++];
                g.map.put(gs, str.toString());
                str = new StringBuilder();
            } else {
                String s = new String(data, i, data[i + 1] == 0 ? 1 : 2);
                str.append(s);
            }
        }
    }
}
