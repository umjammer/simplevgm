package uk.co.omgdrv.simplevgm.fm.nukeykt;

import java.lang.System.Logger;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import uk.co.omgdrv.simplevgm.fm.nukeykt.IYm3438.IYm3438_Type;
import uk.co.omgdrv.simplevgm.model.VgmFmProvider;

import static java.lang.System.getLogger;
import static uk.co.omgdrv.simplevgm.fm.MdFmProvider.FM_STATUS_BUSY_BIT_MASK;
import static uk.co.omgdrv.simplevgm.fm.nukeykt.IYm3438.ym3438_mode_readmode;


/**
 * Ym3438Provider.
 * <p>
 * this class is YM2612 compatible.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-08-30 nsano initial version <br>
 */
public class Ym3438Provider implements VgmFmProvider {

    private static final Logger logger = getLogger(Ym3438Provider.class.getName());

    IYm3438_Type chip = new IYm3438_Type();

    IYm3438 iYm3438 = new Ym3438();

    private final int[][] ym3438_accm = new int[24][2];
    int ym3438_cycles = 0;
    int[] ym3438_sample = new int[2];

    double rateRatioAcc = 0;
    double sampleRateCalcAcc = 0;

    private final Queue<Integer> commandQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong queueSize = new AtomicLong();

    private static final int MASTER_CLOCK_HZ = 7_670_442; //MD_NTSC FM CLOCK
    private static final int CLOCK_HZ = MASTER_CLOCK_HZ / 6;
    private static double CYCLE_PER_MS = CLOCK_HZ / 1000.0;
    private final static double rateRatio = FM_CALCS_PER_MS / CYCLE_PER_MS;

    @Override
    public void reset() {
        iYm3438.OPN2_Reset(chip);
    }

    @Override
    public void init(int clock, int rate) {
        iYm3438.OPN2_SetChipType(ym3438_mode_readmode);
    }

    private boolean isReadyWrite() {
        boolean isBusyState = (iYm3438.OPN2_Read(chip, 0) & FM_STATUS_BUSY_BIT_MASK) > 0;
        boolean isWriteInProgress = iYm3438.isWriteAddrEn(chip) || iYm3438.isWriteDataEn(chip);
        return !isBusyState && !isWriteInProgress;
    }

    private void spinOnce() {
        if (queueSize.get() > 1 && isReadyWrite()) {
            iYm3438.OPN2_Write(chip, commandQueue.poll(), commandQueue.poll());
            queueSize.addAndGet(-2);
        }
        iYm3438.OPN2_Clock(chip, ym3438_accm[ym3438_cycles]);
        ym3438_cycles = (ym3438_cycles + 1) % 24;
        if (ym3438_cycles == 0) {
            ym3438_sample[0] = 0;
            ym3438_sample[1] = 0;
            for (int j = 0; j < 24; j++) {
                ym3438_sample[0] += ym3438_accm[j][0];
                ym3438_sample[1] += ym3438_accm[j][1];
            }
            lastL = (lastL + ym3438_sample[0]) >> 1;
            lastR = (lastR + ym3438_sample[1]) >> 1;
        }
    }

    private int lastL = 0;
    private int lastR = 0;

    @Override
    public void update(int[] buf_lr, int offset, int samples) {
        offset <<= 1;
        sampleRateCalcAcc += samples / rateRatio;
        int total = (int) (sampleRateCalcAcc + 1);
        for (int i = 0; i < total; i++) {
            spinOnce();
            rateRatioAcc += rateRatio;
            if (rateRatioAcc > 1) {
                buf_lr[offset++] = lastL << 4;
                buf_lr[offset++] = lastR << 4;
//logger.log(Level.DEBUG, (lastL << 4) + ", " + (lastR << 4));
                rateRatioAcc--;
            }
        }
        sampleRateCalcAcc -= total;
    }

    @Override
    public void writePort(int addr, int data) {
//logger.log(Level.DEBUG, "addr: " + addr + ", data: " + data);
        commandQueue.offer(addr);
        queueSize.addAndGet(1);
        commandQueue.offer(data);
        queueSize.addAndGet(1);
    }

    @Override
    public int read() {
        return iYm3438.OPN2_Read(chip, 0x4000);
    }
}
