package uk.co.omgdrv.simplevgm.model;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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

    static VgmPsgProvider getProvider(String className) {
        VgmPsgProvider nullProvider = null;
        for (VgmPsgProvider provider : ServiceLoader.load(VgmPsgProvider.class)) {
            if (provider.getClass() == NullVgmPsgProvider.class) {
                nullProvider = provider;
            }
            if (provider.getClass().getName().equals(className)) {
                logger.log(Level.TRACE, "psg: " + provider.getClass());
                return provider;
            }
        }
        if (nullProvider == null) {
            throw new IllegalStateException("no null provider is found");
        }
        logger.log(Level.WARNING, "no such a class: " + className);
        return nullProvider;
    }
}
