package com.lxw.android.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Scroller;


import static android.support.v7.app.AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR;

/**
 * Call attach(Activity activity) to support slide to finish Activity
 *
 * Created by lxw on 2018/3/30.
 */
public class ExitSlider extends LinearLayout {
  private final static String TAG = "ExitSlider";

  //state when dragging by finger
  private static final int STATE_DRAGGING = 1;
  //auto move when finger leave activity
  private static final int STATE_SETTLING = 2;
  //other state
  private static final int STATE_IDLE = 0;

  //the defualt alpha of Left Region when dragging (0-255)
  private static final int DEFAULT_ALPHA = 200;
  //width of shade，in pixel
  private static final int SHADE_WIDTH = 30;

  private static final int LEFT_REGION_COLOR = 0xD0000000;
  private static final int SHADE_COLOR = 0xFF000000;

  //x position when action down or action pointer down
  private PositionRecord mXActionDownRecord = new PositionRecord();
  //y position when action down or action pointer down
  private PositionRecord mYActionDownRecord = new PositionRecord();

  //x position of last Action Event
  private PositionRecord mXLastActionRecord = new PositionRecord();

  private View mOriginView;
  private final int mTouchSlop;
  private VelocityTracker mVelocityTracker;

  private SlideScroller mScroller;
  private int mState = STATE_IDLE;

  private int mActivePointerId = -1;

  private ShapeDrawable mDarkDrawable;
  private Drawable mShadeDrawable;

  //draw Status bar
  StatusBarPainter mStatusBarPainter;

  //make activity support Slide to finish Activity
  public static ExitSlider attach(Activity activity, @ColorInt int statusBarColor) {
    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
    View originView = decorView.getChildAt(0);
    if (originView == null) {
      throw new NullPointerException("originView is null");
    }

    StatusBarPainter statusBarPainter = null;
    if (StatusBarPainter.trySetTransparentStatusBar(activity)) {
      statusBarPainter = new StatusBarPainter(statusBarColor, activity);
    }

    decorView.removeViewAt(0);
    ExitSlider slider = new ExitSlider(activity, originView, statusBarPainter);
    decorView.addView(slider, 0);
    return slider;
  }

  private ExitSlider(Context mContext, View originView, StatusBarPainter statusBarPainter) {
    super(mContext);
    setWillNotDraw(false);
    mOriginView = originView;
    addView(originView);

    ViewConfiguration configuration = ViewConfiguration.get(mContext);
    mTouchSlop = Math.max(configuration.getScaledTouchSlop(), configuration.getScaledPagingTouchSlop());

    mScroller = new SlideScroller(mContext, originView);
    mDarkDrawable = new ShapeDrawable();
    mDarkDrawable.getPaint().setColor(LEFT_REGION_COLOR);
    mShadeDrawable =
        new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] { LEFT_REGION_COLOR, SHADE_COLOR });
    mStatusBarPainter = statusBarPainter;
  }

  //draw status bar
  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    if (mStatusBarPainter != null) {
      mStatusBarPainter.draw(canvas, mOriginView.getLeft(), mOriginView.getRight());
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, mOriginView.getLeft(), t, mOriginView.getLeft() + r, b);
  }

  @Override
  public void computeScroll() {
    super.computeScroll();
    boolean isContinue = mScroller.continueScroll();
    if (isContinue) {
      postInvalidate();

    } else {
      if (mOriginView.getLeft() >= mOriginView.getWidth()) {
        if (getContext() instanceof Activity) {
          ((Activity) getContext()).finish();
        } else {
          throw new IllegalStateException("context not activity");
        }
      }
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (mState != STATE_IDLE && mOriginView.getLeft() < mOriginView.getWidth()) {
      mDarkDrawable.setBounds(0, getTop(), mOriginView.getLeft() - SHADE_WIDTH, getBottom());
      mShadeDrawable.setBounds(mOriginView.getLeft() - SHADE_WIDTH, getTop(), mOriginView.getLeft(), getBottom());
      int alpha =
          (int) (DEFAULT_ALPHA * (mOriginView.getWidth() - mOriginView.getLeft()) / (float) mOriginView.getWidth());
      mDarkDrawable.setAlpha(alpha);
      mShadeDrawable.setAlpha(alpha);
      mDarkDrawable.draw(canvas);
      mShadeDrawable.draw(canvas);
    }
  }

  //make child View has Higher Priority to intercept a MotionEvent
  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    boolean ret = super.dispatchTouchEvent(event);
    if (mState == STATE_IDLE && event.getAction() != MotionEvent.ACTION_DOWN) {
      tryInterceptTouchEvent(event);
    }
    return ret;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      tryInterceptTouchEvent(event);
      return false;
    }
    return mState == STATE_DRAGGING;
  }

  private boolean tryInterceptTouchEvent(MotionEvent event) {
    boolean ret = false;
    int action = event.getActionMasked();
    int pointerIndex = event.getActionIndex();
    int pointerId = event.getPointerId(pointerIndex);
    int eventCount = event.getPointerCount();

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        clearTouch();
        mXActionDownRecord.put(pointerId, event.getX(pointerIndex));
        mYActionDownRecord.put(pointerId, event.getY(pointerIndex));

        break;
      case MotionEvent.ACTION_POINTER_DOWN:
        if (event.getX(pointerIndex) >= mOriginView.getLeft()) {
          mXActionDownRecord.put(pointerId, event.getX(pointerIndex));
          mYActionDownRecord.put(pointerId, event.getY(pointerIndex));
        }
        break;
      case MotionEvent.ACTION_MOVE:
        for (int i = 0; i < eventCount; ++i) {
          int ptId = event.getPointerId(i);
          if (!mXActionDownRecord.contain(ptId)) {
            continue;
          }
          if (!tryDragging(ptId, event.getX(i), event.getY(i))) {
            continue;
          }
          return true;
        }
        break;
      case MotionEvent.ACTION_POINTER_UP:
        mXActionDownRecord.remove(pointerId);
        mYActionDownRecord.remove(pointerId);
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        break;
    }
    return ret;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mState != STATE_DRAGGING) {
      return true;
    }

    int action = event.getActionMasked();
    int pointerIndex = event.getActionIndex();
    int pointerId = event.getPointerId(pointerIndex);
    int eventCount = event.getPointerCount();

    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(event);

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        break;
      case MotionEvent.ACTION_POINTER_DOWN:
        if (event.getX(pointerIndex) >= mOriginView.getLeft()) {
          mXActionDownRecord.put(pointerId, event.getX(pointerIndex));
          mYActionDownRecord.put(pointerId, event.getY(pointerIndex));
        }
        break;
      case MotionEvent.ACTION_MOVE:
        for (int i = 0; i < eventCount; ++i) {
          int ptId = event.getPointerId(i);
          if (!mXActionDownRecord.contain(ptId)) {
            continue;
          }

          float x = event.getX(i);
          if (!tryDragging(ptId, x, event.getY(i))) {
            continue;
          }
          if (mActivePointerId < 0) {
            float actionDownX = mXActionDownRecord.get(ptId);
            if (Math.abs(x - actionDownX) <= mTouchSlop) {
              continue;
            }
            mActivePointerId = ptId;
          }

          if (mActivePointerId != ptId) {
            continue;
          }

          if (mXLastActionRecord.contain(ptId)) {
            float xLast = mXLastActionRecord.get(ptId);
            if (mOriginView.getLeft() + x - xLast < 0) {
              break;
            }
            mOriginView.offsetLeftAndRight((int) (x - xLast));
            invalidate();
          }
          mXLastActionRecord.put(ptId, x);
          break;
        }
        break;
      case MotionEvent.ACTION_POINTER_UP:
        if (!mXActionDownRecord.contain(pointerId)) {
          break;
        }

        mXActionDownRecord.remove(pointerId);
        mYActionDownRecord.remove(pointerId);
        mXLastActionRecord.clear();

        if (mXActionDownRecord.isEmpty()) {
          tryStartScroll(true);
          break;
        }
        if (pointerId == mActivePointerId && tryStartScroll(false)) {
          break;
        }
        mActivePointerId = -1;

        for (int i = 0; i < eventCount; ++i) {
          if (i == pointerIndex) {
            continue;
          }
          int ptId = event.getPointerId(i);
          if (!mXActionDownRecord.contain(ptId)) {
            continue;
          }
          mXActionDownRecord.put(ptId, event.getX(i));
          mYActionDownRecord.put(ptId, event.getY(i));
        }

        break;
      case MotionEvent.ACTION_UP:
        tryStartScroll(true);
        break;
      case MotionEvent.ACTION_CANCEL:
        tryStartScroll(true);
    }
    return true;
  }

  private void clearTouch() {
    if (mState == STATE_IDLE) {
      return;
    }
    mActivePointerId = -1;
    mXActionDownRecord.clear();
    mYActionDownRecord.clear();
    mXLastActionRecord.clear();

    if (mState == STATE_SETTLING && mScroller.abortAnimation()) {
      invalidate();
    }
    setState(STATE_IDLE);

    if (mVelocityTracker != null) {
      mVelocityTracker.recycle();
      mVelocityTracker = null;
    }
  }

  void setState(int state) {
    if (mState == state) {
      return;
    }
    mState = state;
  }

  boolean tryDragging(int pointerId, float x, float y) {
    if (mState == STATE_DRAGGING) {
      return true;
    }
    float xActionDown = mXActionDownRecord.get(pointerId);
    float yActionDown = mYActionDownRecord.get(pointerId);

    //whether move along whith x axis
    if (Math.abs(xActionDown - x) > 2*Math.abs(yActionDown - y)) {
      if (x - xActionDown > mTouchSlop) {
        setState(STATE_DRAGGING);
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
      } else if (x < xActionDown) {
        //update record
        mXActionDownRecord.put(pointerId, x);
        mYActionDownRecord.put(pointerId, y);
      }
    }
    return false;
  }

  /**
   * whether auto move Activity when finger leave the screen
   * force is false, we consider the speed to decide whether start auto move
   * force is true, always start auto move whether speed is zero
   */
  boolean tryStartScroll(boolean force) {
    if (mState == STATE_DRAGGING) {
      mVelocityTracker.computeCurrentVelocity(1000);
      int xSpeed = (int) mVelocityTracker.getXVelocity(mActivePointerId);

      if (mScroller.startScroll(xSpeed, force)) {
        setState(STATE_SETTLING);
        invalidate();
        return true;
      }
    }
    return false;
  }

  //record pointer position
  static final class PositionRecord {
    float[] mPositions;
    private int mPointersDown;

    void put(int id, float x) {
      if (mPositions == null || mPositions.length <= id) {
        float[] positions = new float[id + 1];
        if (mPositions != null) {
          System.arraycopy(mPositions, 0, positions, 0, mPositions.length);
        }
        mPositions = positions;
      }
      mPositions[id] = x;
      mPointersDown |= (1 << id);
      Log.d(TAG, "this: " + this.hashCode() + " put id:" + id + " x: " + x);
    }

    float get(int id) {
      if ((mPointersDown & (1 << id)) == 0) {
        throw new IndexOutOfBoundsException("actionDownX action move before action down");
      }
      return mPositions[id];
    }

    void remove(int id) {
      mPointersDown &= ~(1 << id);
    }

    boolean contain(int id) {
      return (mPointersDown & (1 << id)) != 0;
    }

    void clear() {
      mPointersDown = 0;
    }

    boolean isEmpty() {
      return mPointersDown == 0;
    }
  }

  //deal with auto move when finger leave Activity
  static final class SlideScroller {
    private static final int BASE_SETTLE_DURATION = 256; // ms
    private static final int MAX_SETTLE_DURATION = 600; // ms
    private final int mMaxVelocity;
    private final int mMinVelocity;
    private final View mOriginView;
    Scroller mScroller;

    SlideScroller(Context context, View originView) {
      ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
      mMaxVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
      mMinVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
      mOriginView = originView;

      mScroller = new Scroller(context, new Interpolator() {
        @Override
        public float getInterpolation(float t) {
          t -= 1.0f;
          return t * t * t * t * t + 1.0f;
        }
      });
    }

    final boolean startScroll(int xSpeed, boolean force) {
      xSpeed = limitSpeed(xSpeed, force);
      if (xSpeed == 0) return false;

      Log.d(TAG, "setState speed:" + xSpeed);
      int dx = -mOriginView.getLeft();
      if (xSpeed > 0) {
        dx = mOriginView.getWidth() - mOriginView.getLeft();
      }
      Log.d(TAG, "setState speed:" + xSpeed + " dx: " + dx + " left:" + mOriginView.getLeft());
      mScroller.startScroll(mOriginView.getLeft(), mOriginView.getTop(), dx, 0,
                            computeAxisDuration(dx, xSpeed, mOriginView.getWidth()));
      return true;
    }

    final boolean continueScroll() {
      boolean isScrolling = mScroller.computeScrollOffset();
      Log.d(TAG, "setState: isScrolling " + isScrolling + " x " + mScroller.getCurrX() + " isScroll:" + isScrolling);
      if (isScrolling) {
        final int x = mScroller.getCurrX();
        mOriginView.offsetLeftAndRight((int) (x - mOriginView.getLeft()));
      }

      return isScrolling;
    }

    final boolean abortAnimation() {
      boolean isScrolling = mScroller.computeScrollOffset();
      Log.d(TAG, "setState: isScrolling " + isScrolling + " x " + mScroller.getCurrX() + " isScroll:" + isScrolling);
      if (isScrolling) {
        final int x = mScroller.getFinalX();
        mOriginView.offsetLeftAndRight((int) (x - mOriginView.getLeft()));
        mScroller.abortAnimation();
      }
      return isScrolling;
    }

    private int computeAxisDuration(int delta, int xvel, int length) {
      final int velocity = Math.abs(xvel);
      if (delta == 0) {
        return 0;
      }
      final int halfLength = length / 2;
      final float distanceRatio = Math.min(1f, (float) Math.abs(delta) / length);
      final float distance = halfLength + halfLength * distanceInfluenceForSnapDuration(distanceRatio);
      int duration;
      if (velocity > 0) {
        duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
      } else {
        final float range = (float) Math.abs(delta) / length;
        duration = (int) ((range + 1) * BASE_SETTLE_DURATION);
      }
      return Math.min(duration, MAX_SETTLE_DURATION);
    }

    private float distanceInfluenceForSnapDuration(float f) {
      f -= 0.5f; // center the values about 0.
      f *= 0.3f * Math.PI / 2.0f;
      return (float) Math.sin(f);
    }

    private int limitSpeed(int value, boolean force) {
      final int absValue = Math.abs(value);
      if (absValue < mMinVelocity) {
        if (!force) return 0;

        if (6 * mOriginView.getLeft() > mOriginView.getWidth()) {
          return mMinVelocity;
        }
        return -mMinVelocity;
      }
      if (absValue > mMaxVelocity) return value > 0 ? mMaxVelocity : -mMaxVelocity;
      return value;
    }
  }

  //show status bar
  private static final class StatusBarPainter {
    ColorDrawable mDrawable;
    private int mStatusBarHeight = -1;

    StatusBarPainter(@ColorInt int color, Context context) {
      mStatusBarHeight = getStatusBarHeight(context);
      mDrawable = new ColorDrawable(color);
    }

    void draw(Canvas canvas, int left, int right) {
      mDrawable.setBounds(left, 0, right, mStatusBarHeight);
      mDrawable.draw(canvas);
    }

    static boolean trySetTransparentStatusBar(Activity activity) {
      if (!(activity instanceof AppCompatActivity)) {
        //BeCause We need to Decide Whether Action Bar Exist, We just Support AppCompatActivity。
        throw new IndexOutOfBoundsException("not support other Activity");
      }
      boolean isTransparent = false;

      //5.0 or later
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        isTransparent = true;

        //4.4 to 5.0
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        WindowManager.LayoutParams localLayoutParams = activity.getWindow().getAttributes();
        localLayoutParams.flags = (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | localLayoutParams.flags);
        isTransparent = true;

        /**
         * In this case we need to change the top of content View  by ourselves.
         * */
        int paddingTop = getStatusBarHeight(activity);

        //If Exist ActionBar in this Activity。
        if (((AppCompatActivity) activity).getDelegate().hasWindowFeature(FEATURE_SUPPORT_ACTION_BAR)) {
          paddingTop += getActionBarHeight(activity);
        }
        View contentView = activity.findViewById(android.R.id.content);
        contentView.setPadding(contentView.getPaddingLeft(), contentView.getPaddingTop() + paddingTop,
                               contentView.getPaddingRight(), contentView.getPaddingBottom());
      }
      return isTransparent;
    }

    static int getActionBarHeight(Context context) {
      TypedValue tv = new TypedValue();
      if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
        return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
      }
      return 0;
    }

    static int getStatusBarHeight(Context context) {
      Resources resources = context.getResources();
      int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
      return resources.getDimensionPixelSize(resourceId);
    }
  }
}