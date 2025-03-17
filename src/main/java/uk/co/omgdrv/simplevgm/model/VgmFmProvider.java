package uk.co.omgdrv.simplevgm.model;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import uk.co.omgdrv.simplevgm.VgmEmu;

import static java.lang.System.getLogger;


/**
 * VgmFmProvider.
 *
 * @author Federico Berti
 * @version Copyright 2019
 */
public interface VgmFmProvider {

    Logger logger = getLogger(VgmFmProvider.class.getName());

    double FM_CALCS_PER_MS = VgmEmu.VGM_SAMPLE_RATE_HZ / 1000.0;

    void reset();

    void init(int clock, int rate);

    void update(int[] buf_lr, int offset, int end);

    default int readRegister(int type, int regNumber) {
        throw new IllegalStateException("implement code in this subclass");
    }

    default void writePort(int addr, int data) {
        throw new RuntimeException("implement code in this subclass");
    }

    // single port
    default void write(int addr, int data) {
        throw new RuntimeException("implement code in this subclass");
    }

    default int read() {
        throw new RuntimeException("implement code in this subclass");
    }

    ServiceLoader<VgmFmProvider> serviceLoader = ServiceLoader.load(VgmFmProvider.class);

    /** @throws NoSuchElementException no suitable provider is found */
    static VgmFmProvider getProvider(String name) {
        for (VgmFmProvider provider : serviceLoader) {
            if (name != null && !name.isEmpty() && provider.getClass().getName().toLowerCase().contains(name.toLowerCase())) {
logger.log(Level.TRACE, "fm: " + provider.getClass());
                return provider;
            }
        }
        throw new NoSuchElementException("no provider found for: " + name);
    }
}
