package com.termux.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * Living Liquid Glass Backdrop View.
 * Renders a deep warm graphite base (#0E0F11) with 3 soft-drifting warm light orbs
 * and a faint terminal scanline grid. Includes subtle smoothed gyroscope parallax.
 * Provides the optical background required for real glass refraction and specular read.
 */
public class LiquidBackdropView extends View implements SensorEventListener {

    private final Paint mBasePaint = new Paint();
    private final Paint mOrbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static class LightOrb {
        float xNorm, yNorm;
        float radiusDp;
        int color;
        float speedX, speedY;
        float phase;

        LightOrb(float xNorm, float yNorm, float radiusDp, int color, float speedX, float speedY, float phase) {
            this.xNorm = xNorm;
            this.yNorm = yNorm;
            this.radiusDp = radiusDp;
            this.color = color;
            this.speedX = speedX;
            this.speedY = speedY;
            this.phase = phase;
        }
    }

    private final LightOrb[] mOrbs = new LightOrb[]{
        new LightOrb(0.25f, 0.20f, 220f, Color.argb(16, 242, 237, 228), 0.0003f, 0.0002f, 0f),      // Warm cream
        new LightOrb(0.80f, 0.45f, 260f, Color.argb(14, 205, 184, 154), -0.00025f, 0.00035f, 1.5f),  // Champagne
        new LightOrb(0.35f, 0.75f, 200f, Color.argb(12, 61, 220, 151), 0.0002f, -0.0003f, 3.0f)     // Mint hint
    };

    private ValueAnimator mDriftAnimator;
    private float mAnimProgress = 0f;
    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private float mTargetParallaxX = 0f;
    private float mTargetParallaxY = 0f;
    private float mCurrentParallaxX = 0f;
    private float mCurrentParallaxY = 0f;
    private boolean mIsActive = false;

    public LiquidBackdropView(Context context) {
        this(context, null);
    }

    public LiquidBackdropView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mBasePaint.setColor(0xFF0E0F11);

        mGridPaint.setColor(Color.argb(5, 255, 255, 255));
        mGridPaint.setStrokeWidth(1f);

        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (mRotationSensor == null) {
                mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
        }

        mDriftAnimator = ValueAnimator.ofFloat(0f, 1f);
        mDriftAnimator.setDuration(30000); // 30s slow smooth cycle
        mDriftAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mDriftAnimator.setRepeatMode(ValueAnimator.RESTART);
        mDriftAnimator.setInterpolator(new LinearInterpolator());
        mDriftAnimator.addUpdateListener(animation -> {
            mAnimProgress = (float) animation.getAnimatedValue();
            // Smoothly interpolate gyro parallax
            mCurrentParallaxX += (mTargetParallaxX - mCurrentParallaxX) * 0.08f;
            mCurrentParallaxY += (mTargetParallaxY - mCurrentParallaxY) * 0.08f;
            invalidate();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            start();
        } else {
            stop();
        }
    }

    public void start() {
        if (mIsActive) return;
        mIsActive = true;
        if (mDriftAnimator != null && !mDriftAnimator.isStarted()) {
            mDriftAnimator.start();
        }
        if (mSensorManager != null && mRotationSensor != null) {
            mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stop() {
        if (!mIsActive) return;
        mIsActive = false;
        if (mDriftAnimator != null && mDriftAnimator.isStarted()) {
            mDriftAnimator.cancel();
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Deep Graphite base
        canvas.drawRect(0, 0, w, h, mBasePaint);

        float density = getResources().getDisplayMetrics().density;

        // 2. Drifting soft orbs with gyro parallax
        for (LightOrb orb : mOrbs) {
            float time = mAnimProgress * (float) (Math.PI * 2);
            float offsetX = (float) Math.sin(time + orb.phase) * (35f * density);
            float offsetY = (float) Math.cos(time + orb.phase) * (45f * density);

            float cx = (orb.xNorm * w) + offsetX + mCurrentParallaxX;
            float cy = (orb.yNorm * h) + offsetY + mCurrentParallaxY;
            float r = orb.radiusDp * density;

            RadialGradient gradient = new RadialGradient(
                cx, cy, r,
                orb.color, Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            );
            mOrbPaint.setShader(gradient);
            canvas.drawCircle(cx, cy, r, mOrbPaint);
        }

        // 3. Faint subtle scanline grid (draw lines every 6dp)
        float step = 6f * density;
        for (float y = 0; y < h; y += step) {
            canvas.drawLine(0, y, w, y, mGridPaint);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null) return;
        float density = getResources().getDisplayMetrics().density;
        float maxOffset = 18f * density;

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float tiltX = event.values[0];
            float tiltY = event.values[1];
            mTargetParallaxX = Math.max(-maxOffset, Math.min(maxOffset, -tiltY * 30f * density));
            mTargetParallaxY = Math.max(-maxOffset, Math.min(maxOffset, tiltX * 30f * density));
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float ax = event.values[0];
            float ay = event.values[1];
            mTargetParallaxX = Math.max(-maxOffset, Math.min(maxOffset, -ax * 3f * density));
            mTargetParallaxY = Math.max(-maxOffset, Math.min(maxOffset, ay * 3f * density));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op
    }
}
