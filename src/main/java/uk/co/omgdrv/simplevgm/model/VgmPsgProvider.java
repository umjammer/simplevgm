package uk.co.omgdrv.simplevgm.model;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import libgme.util.BlipBuffer;

import static java.lang.System.getLogger;


/**
 * VgmPsgProvider.
 *
 * @author Federico Berti
 * @version Copyright 2019
 */
public interface VgmPsgProvider {

    Logger logger = getLogger(VgmPsgProvider.class.getName());

    void writeData(int time, int data);

    void setOutput(BlipBuffer center, BlipBuffer left, BlipBuffer right);

    void reset();

    void writeGG(int time, int data);

    void endFrame(int endTime);

    ServiceLoader<VgmPsgProvider> serviceLoader = ServiceLoader.load(VgmPsgProvider.class);

    static VgmPsgProvider getProvider(String name) {
        for (VgmPsgProvider provider : serviceLoader) {
            if (name != null && !name.isEmpty() && provider.getClass().getName().toLowerCase().contains(name.toLowerCase())) {
logger.log(Level.TRACE, "psg: " + provider.getClass());
                return provider;
            }
        }
        throw new NoSuchElementException("no provider found for: " + name);
    }
}
