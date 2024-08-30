import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import libgme.VGMPlayer;
import uk.co.omgdrv.simplevgm.VgmEmu;
import uk.co.omgdrv.simplevgm.psg.PsgCompare;
import uk.co.omgdrv.simplevgm.util.Util;


/**
 * Runner.
 *
 * @author Federico Berti
 * @version Copyright 2019
 */
public class Runner {

    static {
        System.setProperty("libgme.BlipBuffer.muchFaster", "true");
    }

    private static final boolean DISABLE_PSG = false;
    private static final String VGM_FOLDER = "vgm";
    private static final String VGM_FILE = "vgm/adv_batman_01.vgz";
    private static final boolean runPsgCompare = false;

    private static final Predicate<Path> vgmFilesPredicate = p ->
            p.toString().endsWith(".vgm") || p.toString().endsWith(".vgz");

    public static void main(String[] args) throws Exception {
        Path path = getPathToPlay(args);
        boolean isFolder = path.toFile().isDirectory();
        System.out.printf("Playing %s: %s%n",
                (isFolder ? "folder" : "file"), path.toAbsolutePath());
        if (runPsgCompare) System.setProperty("uk.co.omgdrv.simplevgm.psg", PsgCompare.class.getName());
        VGMPlayer v = new VGMPlayer(VgmEmu.VGM_SAMPLE_RATE_HZ);
//        ((ClassicEmu) v.getEmu()).getBuf().setObserver(new BlipHelper("SmsApu", false));
        if (isFolder) {
            playRecursive(v, path);
        } else {
            playOne(v, path);
        }
    }

    private static Path getPathToPlay(String[] args) {
        String s = VGM_FILE != null ? VGM_FILE : (VGM_FOLDER != null ? VGM_FOLDER : ".");
        Path p = Paths.get(s);
        if (args.length > 0) {
            p = Paths.get(args[0]);
        }
        return p;
    }

    public static List<Path> getRecursiveVgmFiles(Path folder) throws IOException {
        Set<Path> fileSet = new HashSet<>();
        Files.walkFileTree(folder, createFileVisitor(fileSet));
        List<Path> list = new ArrayList<>(fileSet);
        Collections.shuffle(list);
        System.out.println("VGM files found: " + fileSet.size());
        return list;
    }

    private static void playAll(VGMPlayer v, String folderName) throws Exception {
        Path folder = Paths.get(".", folderName);
        List<Path> files = Files.list(folder).filter(vgmFilesPredicate).sorted().toList();
        files.forEach(f -> playOne(v, f));
    }

    public static void playRecursive(VGMPlayer v, Path folder) throws Exception {
        getRecursiveVgmFiles(folder).forEach(f -> playOne(v, f));
    }

    private static void playOne(VGMPlayer v, Path file) {
        try {
            System.out.println("Playing: " + file.toAbsolutePath());
            v.loadFile(file.toAbsolutePath().toString());
            v.startTrack(0);
            waitForCompletion(v);
            v.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void waitForCompletion(VGMPlayer v) {
        do {
            Util.sleep(1000);
        } while (v.isPlaying());
    }

    private static FileVisitor<Path> createFileVisitor(Set<Path> fileSet) {
        return new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (vgmFilesPredicate.test(file)) {
                    fileSet.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        };
    }
}
