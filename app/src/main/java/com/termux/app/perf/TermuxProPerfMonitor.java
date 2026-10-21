package com.termux.app.perf;

import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-precision Choreographer-based Frame & Jank Monitor for Termux Pro.
 * Tracks frame times, jank rates (>16.6ms for 60Hz and >8.3ms for 120Hz),
 * and computes p50, p95, p99 percentiles for real scenario benchmarking.
 */
public final class TermuxProPerfMonitor {

    private static final String LOG_TAG = "TermuxProPerf";
    private static TermuxProPerfMonitor sInstance;

    private boolean mIsRunning = false;
    private long mLastFrameTimeNanos = 0;

    private final List<Float> mCurrentDurations = new ArrayList<>(2048);
    private final Map<String, ScenarioStats> mScenarioResults = new HashMap<>();
    private String mActiveScenario = "default";

    public static class ScenarioStats {
        public final String name;
        public final int totalFrames;
        public final int jankFrames60Hz;
        public final int jankFrames120Hz;
        public final float p50Ms;
        public final float p95Ms;
        public final float p99Ms;
        public final float jankPercent60Hz;
        public final float jankPercent120Hz;

        public ScenarioStats(String name, int total, int jank60, int jank120, float p50, float p95, float p99) {
            this.name = name;
            this.totalFrames = total;
            this.jankFrames60Hz = jank60;
            this.jankFrames120Hz = jank120;
            this.p50Ms = p50;
            this.p95Ms = p95;
            this.p99Ms = p99;
            this.jankPercent60Hz = total > 0 ? (jank60 * 100f / total) : 0f;
            this.jankPercent120Hz = total > 0 ? (jank120 * 100f / total) : 0f;
        }

        @Override
        public String toString() {
            return String.format(java.util.Locale.US,
                "Scenario [%s]: %d frames | p50=%.1fms | p95=%.1fms | p99=%.1fms | Jank(>16.6ms)=%.1f%% | Jank(>8.3ms)=%.1f%%",
                name, totalFrames, p50Ms, p95Ms, p99Ms, jankPercent60Hz, jankPercent120Hz);
        }
    }

    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!mIsRunning) return;

            if (mLastFrameTimeNanos > 0) {
                long durationNanos = frameTimeNanos - mLastFrameTimeNanos;
                float durationMs = durationNanos / 1_000_000f;
                // Clamp unreasonable pauses (e.g. background suspension)
                if (durationMs > 0.1f && durationMs < 500f) {
                    synchronized (mCurrentDurations) {
                        mCurrentDurations.add(durationMs);
                    }
                }
            }
            mLastFrameTimeNanos = frameTimeNanos;
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    public static synchronized TermuxProPerfMonitor getInstance() {
        if (sInstance == null) {
            sInstance = new TermuxProPerfMonitor();
        }
        return sInstance;
    }

    private TermuxProPerfMonitor() {}

    public synchronized void start(String scenarioName) {
        mActiveScenario = scenarioName != null ? scenarioName : "benchmark";
        synchronized (mCurrentDurations) {
            mCurrentDurations.clear();
        }
        mLastFrameTimeNanos = 0;
        mIsRunning = true;
        new Handler(Looper.getMainLooper()).post(() -> {
            Choreographer.getInstance().removeFrameCallback(mFrameCallback);
            Choreographer.getInstance().postFrameCallback(mFrameCallback);
        });
        Logger.logInfo(LOG_TAG, "Perf tracking started for scenario: " + mActiveScenario);
    }

    public synchronized ScenarioStats stop() {
        mIsRunning = false;
        new Handler(Looper.getMainLooper()).post(() -> Choreographer.getInstance().removeFrameCallback(mFrameCallback));

        List<Float> copy;
        synchronized (mCurrentDurations) {
            copy = new ArrayList<>(mCurrentDurations);
        }

        int total = copy.size();
        if (total == 0) {
            ScenarioStats empty = new ScenarioStats(mActiveScenario, 0, 0, 0, 0, 0, 0);
            mScenarioResults.put(mActiveScenario, empty);
            return empty;
        }

        Collections.sort(copy);
        int jank60 = 0;
        int jank120 = 0;
        for (float dur : copy) {
            if (dur > 16.66f) jank60++;
            if (dur > 8.33f) jank120++;
        }

        float p50 = copy.get((int) (total * 0.50f));
        float p95 = copy.get(Math.min((int) (total * 0.95f), total - 1));
        float p99 = copy.get(Math.min((int) (total * 0.99f), total - 1));

        ScenarioStats stats = new ScenarioStats(mActiveScenario, total, jank60, jank120, p50, p95, p99);
        mScenarioResults.put(mActiveScenario, stats);
        Logger.logInfo(LOG_TAG, stats.toString());
        return stats;
    }

    public synchronized ScenarioStats getStats(String scenarioName) {
        return mScenarioResults.get(scenarioName);
    }

    public synchronized Map<String, ScenarioStats> getAllStats() {
        return new HashMap<>(mScenarioResults);
    }
}
