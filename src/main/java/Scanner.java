import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import uk.co.omgdrv.simplevgm.model.VgmHeader;
import uk.co.omgdrv.simplevgm.util.Util;

import static java.lang.System.getLogger;


/**
 * Scanner.
 * <p>
 * system properties
 * <ul>
 *  <li>simplevgm.Scanner.vgm.folder ... vgm folder to play</li>
 * </ul>
 *
 * @author Federico Berti
 * @version Copyright 2019
 */
public class Scanner {

    private static final Logger logger = getLogger(Scanner.class.getName());

    private static final String VGM_FOLDER = System.getProperty("simplevgm.Scanner.vgm.folder", "/data/emu/vgm");

    public static void main(String[] args) throws Exception {
        Path p = Paths.get(VGM_FOLDER);
        Runner.getRecursiveVgmFiles(p).forEach(Scanner::printVgmHeader);
    }

    private static void printVgmHeader(Path p) {
        String filepath = p.toAbsolutePath().toString();
        try {
            byte[] data = Util.readFile(p.toAbsolutePath().toString());
            VgmHeader vgmHeader = VgmHeader.loadHeader(data);
            if (vgmHeader.getYm2612Clk() == 0 && vgmHeader.getVersion() <= 0x101) {
                logger.log(Level.DEBUG, "File: " + filepath);
                logger.log(Level.DEBUG, vgmHeader + "\n");
            }
        } catch (Exception e) {
            logger.log(Level.DEBUG, "File: " + filepath + ", " + e.getMessage());
        }
    }
}
