package to.casorin.fountain;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResetTrackerTest {
    @Test public void confirmsDropUsingThreeIndependentFrames() {
        ResetTracker t = new ResetTracker();
        assertNull(t.observe(31600L, 100, 10000));
        assertNull(t.observe(34000L, 500, 10400));
        assertNull(t.observe(5000L, 1000, 10900));
        assertNull(t.observe(5200L, 1400, 11300));
        ResetTracker.Event e = t.observe(5500L, 1800, 11700);
        assertNotNull(e);
        assertEquals(34000, e.previousFund());
        assertEquals(10900, e.observedAt());
        assertNull(e.intervalMs());
    }
    @Test public void duplicateCaptureCannotConfirmReset() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L, 100, 10000);
        t.observe(5000L, 200, 10100);
        for (int i = 0; i < 10; i++) assertNull(t.observe(5000L, 200, 10100));
    }
    @Test public void shortOverlapKeepsOnlyVerifiedDropReadings() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L, 100, 10000);
        t.observe(5000L, 200, 10100);
        t.observe(null, 300, 10200);
        assertNull(t.observe(6000L, 400, 10300));
        assertNotNull(t.observe(6000L, 500, 10400));
    }
    @Test public void unreadableFramesCannotConfirmDrop() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L,100,10000); t.observe(5000L,200,10100);
        for (int i=3;i<20;i++) assertNull(t.observe(null,i*100,10000+i*100));
        assertEquals(1,t.confirmationHits());
        assertEquals(0,t.lastReset);
    }
    @Test public void confirmationWindowExpiresEvenAcrossReadableFrames() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L,100,10000); t.observe(5000L,200,10100);
        assertNull(t.observe(5100L,1600,11500));
        assertNull(t.observe(5200L,2701,12601));
        assertEquals(1,t.confirmationHits());
        assertNull(t.observe(5300L,3000,12900));
        assertNotNull(t.observe(5400L,3300,13200));
    }
    @Test public void longOverlapDiscardsOldCandidate() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L,100,10000); t.observe(5000L,200,10100);
        t.observe(null,2701,12601);
        assertEquals(0,t.confirmationHits());
        assertNull(t.observe(6000L,2800,12700));
        assertNull(t.observe(6100L,3000,12900));
        ResetTracker.Event e=t.observe(6200L,3200,13100);
        assertEquals(12700,e.observedAt());
    }
    @Test public void intermittentOverlapDoesNotDelayConfirmationUntilClicksStop() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L,100,10000);
        t.observe(5000L,700,10600);
        t.observe(null,850,10750);
        t.observe(5100L,1300,11200);
        t.observe(null,1450,11350);
        ResetTracker.Event e=t.observe(5200L,1900,11800);
        assertNotNull(e);
        assertEquals(10600,e.observedAt());
        assertEquals(60000,e.previousFund());
    }
    @Test public void recoveryAfterOverlapStillCancelsCandidate() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L,100,10000); t.observe(5000L,200,10100);
        t.observe(null,300,10200); t.observe(60000L,400,10300);
        assertNull(t.observe(5000L,500,10400));
        assertNull(t.observe(5100L,600,10500));
    }
    @Test public void longGapDoesNotInventReset() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L, 100, 10000);
        assertNull(t.observe(5000L, 20000, 30000));
        assertNull(t.observe(5000L, 20500, 30500));
        assertNull(t.observe(5000L, 21000, 31000));
    }
    @Test public void highOutlierCannotBecomePreviousFund() {
        ResetTracker t = new ResetTracker();
        t.observe(31600L, 100, 10000);
        t.observe(1201656L, 200, 10100);
        t.observe(32000L, 300, 10200);
        t.observe(5000L, 400, 10300);
        t.observe(5100L, 500, 10400);
        assertEquals(32000, t.observe(5200L, 600, 10500).previousFund());
    }
    @Test public void restartDoesNotInventInterval() {
        ResetTracker t = new ResetTracker();
        t.lastReset = 1000;
        t.restartSession();
        assertEquals(0, t.lastReset);
    }
    @Test public void ordinaryGrowthAndSmallCorrectionsAreNotResets() {
        ResetTracker t = new ResetTracker();
        long frame = 1;
        for (long value : new long[]{10000,12000,16000,15000,17000,20000,19500,21000}) {
            assertNull(t.observe(value, frame++, frame*1000));
        }
    }
    @Test public void sameLowEpisodeCannotCreateRepeatedRows() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L,100,10000); t.observe(5000L,200,10100); t.observe(5100L,300,10200);
        assertNotNull(t.observe(5200L,400,10300));
        for (int i = 5; i < 15; i++) assertNull(t.observe(5200L,i*100,i*100+10000));
    }
    @Test public void secondResetHasKnownIntervalInContinuousSession() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L,100,10000); t.observe(5000L,200,10100); t.observe(5100L,300,10200);
        t.observe(5200L,400,10300); t.observe(10000L,500,10400); t.observe(30000L,600,10500);
        t.observe(5000L,700,10600); t.observe(5100L,800,10700);
        ResetTracker.Event e = t.observe(5200L,900,10800);
        assertNotNull(e); assertEquals(Long.valueOf(500),e.intervalMs()); assertEquals(30000,e.previousFund());
    }
    @Test public void intervalIsUnknownAfterVisibilityGap() {
        ResetTracker t = new ResetTracker(); t.lastReset = 10000;
        t.observe(60000L,100,10100); t.observe(20000L,20000,30000);
        assertEquals(0,t.lastReset);
        t.observe(5000L,21000,31000); t.observe(5100L,22000,32000);
        assertNull(t.observe(5200L,23000,33000).intervalMs());
    }
    @Test public void recoveredFundCancelsUnconfirmedDrop() {
        ResetTracker t = new ResetTracker();
        t.observe(60000L,100,10000); t.observe(5000L,200,10100); t.observe(60000L,300,10200);
        assertNull(t.observe(5000L,400,10300)); assertNull(t.observe(5200L,500,10400));
    }
}
