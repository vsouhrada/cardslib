/*
 * ******************************************************************************
 *   Copyright (c) 2013 Roman Nurik, Jake Wharton, Vaclav Souhrada, Gabriele Mariotti.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package it.gmariotti.cardslib.library.view.listener;

import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;

import static com.nineoldandroids.view.ViewHelper.setAlpha;
import static com.nineoldandroids.view.ViewHelper.setTranslationX;
import static com.nineoldandroids.view.ViewPropertyAnimator.animate;


/**
 * <p>
 * It is based on Jake Wharton & Roman Nurik code.
 * See this link for original code:<br>
 * <a href="https://github.com/JakeWharton/SwipeToDismissNOA">SwipeToDismissNOA</a> &
 * <a href="https://github.com/romannurik/Android-SwipeToDismiss">Android-SwipeToDismiss</a>
 * </p>
 * </p>
 * It provides a SwipeDismissViewTouchListener for a CardList.
 * </p>
 * <p/>
 * <p/>
 * A {@link android.view.View.OnTouchListener} that makes the list items in a {@link android.widget.ListView}
 * dismissable. {@link android.widget.ListView} is given special treatment because by default it handles touches
 * for its list items... i.e. it's in charge of drawing the pressed state (the list selector),
 * handling list item clicks, etc.
 * <p/>
 * <p>After creating the listener, the caller should also call
 * {@link android.widget.ListView#setOnScrollListener(android.widget.AbsListView.OnScrollListener)}, passing
 * in the scroll listener returned by {@link #makeScrollListener()}. If a scroll listener is
 * already assigned, the caller should still pass scroll changes through to this listener. This will
 * ensure that this {@link it.gmariotti.cardslib.library.view.listener.SwipeDismissListViewTouchListener} is paused during list view
 * scrolling.</p>
 * <p/>
 * <p>Example usage:</p>
 * <p/>
 * <pre>
 * SwipeDismissListViewTouchListener touchListener =
 *         new SwipeDismissListViewTouchListener(
 *                 listView,
 *                 new SwipeDismissListViewTouchListener.IOnDismissCallback() {
 *                     public void onDismiss(ListView listView, int[] reverseSortedPositions) {
 *                         for (int position : reverseSortedPositions) {
 *                             adapter.remove(adapter.getItem(position));
 *                         }
 *                         adapter.notifyDataSetChanged();
 *                     }
 *                 });
 * listView.setOnTouchListener(touchListener);
 * listView.setOnScrollListener(touchListener.makeScrollListener());
 * </pre>
 * <p/>
 * <p>This class Requires API level 12 or later due to use of {@link
 * android.view.ViewPropertyAnimator}.</p>
 * <p/>
 * <p>For a generalized {@link android.view.View.OnTouchListener} that makes any view dismissable,
 * see {@link SwipeDismissViewTouchListener}.</p>
 *
 * @see SwipeDismissViewTouchListener
 */
public class SwipeDismissListViewTouchListener implements View.OnTouchListener {

  // Cached ViewConfiguration and system-wide constant values
  private int slop;
  private int minFlingVelocity;
  private int maxFlingVelocity;

  private long animationTime;

  // Fixed properties
  private ListView listView;

  private IOnDismissCallback callback;

  private int viewWidth = 1; // 1 and not 0 to prevent dividing by zero

  // Transient properties
  private List<PendingDismissData> pendingDismisses = new ArrayList<PendingDismissData>();

  private int dismissAnimationRefCount = 0;

  private float downX;
  private float downY;

  private boolean swiping;
  private int swipingSlop;

  private VelocityTracker velocityTracker;

  private int downPosition;

  private View downView;

  private boolean paused;

  /**
   * The callback interface used by {@link SwipeDismissListViewTouchListener} to inform its client
   * about a successful dismissal of one or more list item positions.
   */
  public interface IOnDismissCallback {

    /**
     * Called to determine whether the given position can be dismissed.
     */
    boolean canDismiss(int position, Card card);


    /**
     * Called when the user has indicated they she would like to dismiss one or more list item
     * positions.
     *
     * @param listView               The originating {@link android.widget.ListView}.
     * @param reverseSortedPositions An array of positions to dismiss, sorted in descending
     *                               order for convenience.
     */
    void onDismiss(ListView listView, int[] reverseSortedPositions);
  }

  /**
   * Constructs a new swipe-to-dismiss touch listener for the given list view.
   *
   * @param listView The list view whose items should be dismissable.
   * @param callback The callback to trigger when the user has indicated that she would like to
   *                 dismiss one or more list items.
   */
  public SwipeDismissListViewTouchListener(ListView listView, IOnDismissCallback callback) {
    ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
    slop = vc.getScaledTouchSlop();
    minFlingVelocity = vc.getScaledMinimumFlingVelocity();
    maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
    animationTime = listView.getContext().getResources().getInteger(
            android.R.integer.config_shortAnimTime);
    this.listView = listView;
    this.callback = callback;
  }

  /**
   * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
   *
   * @param enabled Whether or not to watch for gestures.
   */
  public void setEnabled(boolean enabled) {
    paused = !enabled;
  }

  /**
   * Returns an {@link android.widget.AbsListView.OnScrollListener} to be added to the
   * {@link android.widget.ListView} using
   * {@link android.widget.ListView#setOnScrollListener(android.widget.AbsListView.OnScrollListener)}.
   * If a scroll listener is already assigned, the caller should still pass scroll changes
   * through to this listener. This will ensure that this
   * {@link SwipeDismissListViewTouchListener} is paused during list view scrolling.</p>
   *
   * @see {@link SwipeDismissListViewTouchListener}
   */
  public AbsListView.OnScrollListener makeScrollListener() {
    return new AbsListView.OnScrollListener() {

      @Override
      public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
      }

      @Override
      public void onScroll(AbsListView absListView, int i, int i1, int i2) {
      }
    };
  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    if (viewWidth < 2) {
      viewWidth = listView.getWidth();
    }

    switch (motionEvent.getActionMasked()) {
      case MotionEvent.ACTION_DOWN: {
        if (paused) {
          return false;
        }

        // TODO: ensure this is a finger, and set a flag

        // Find the child view that was touched (perform a hit test)
        Rect rect = new Rect();
        int childCount = listView.getChildCount();
        int[] listViewCoords = new int[2];
        listView.getLocationOnScreen(listViewCoords);
        int x = (int) motionEvent.getRawX() - listViewCoords[0];
        int y = (int) motionEvent.getRawY() - listViewCoords[1];
        View child;
        for (int i = 0; i < childCount; i++) {
          child = listView.getChildAt(i);
          child.getHitRect(rect);
          if (rect.contains(x, y)) {
            downView = child;
            break;
          }
        }

        if (downView != null) {
          downX = motionEvent.getRawX();
          downY = motionEvent.getRawY();
          downPosition = listView.getPositionForView(downView);

          if (callback.canDismiss(downPosition,(Card) listView.getAdapter().getItem(downPosition))) {
            velocityTracker = VelocityTracker.obtain();
            velocityTracker.addMovement(motionEvent);
          } else {
            downView = null;
          }
        }
        view.onTouchEvent(motionEvent);

        return true;
      }

      case MotionEvent.ACTION_UP: {
        if (velocityTracker == null) {
          break;
        }

        float deltaX = motionEvent.getRawX() - downX;
        velocityTracker.addMovement(motionEvent);
        velocityTracker.computeCurrentVelocity(1000);
        float velocityX = Math.abs(velocityTracker.getXVelocity());
        float velocityY = Math.abs(velocityTracker.getYVelocity());
        boolean dismiss = false;
        boolean dismissRight = false;
        if (Math.abs(deltaX) > viewWidth / 2 && swiping) {
          dismiss = true;
          dismissRight = deltaX > 0;
        } else if (minFlingVelocity <= velocityX && velocityX <= maxFlingVelocity
                && velocityY < velocityX && swiping) {
          dismiss = true;
          dismissRight = velocityTracker.getXVelocity() > 0;
        }
        if (dismiss && downPosition != ListView.INVALID_POSITION) {
          // dismiss
          dismiss(downView, downPosition, dismissRight);
        } else {
          // cancel
          animate(downView)
                  .translationX(0)
                  .alpha(1)
                  .setDuration(animationTime)
                  .setListener(null);
        }

        velocityTracker = null;
        downX = 0;
        downY = 0;
        downView = null;
        downPosition = ListView.INVALID_POSITION;

        if (swiping){
          // Cancel ListView's touch (un-highlighting the item)
          /*MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
          cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                  (motionEvent.getActionIndex()
                          << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
          listView.onTouchEvent(cancelEvent);
          cancelEvent.recycle();*/

          //To prevent onClick event with a fast swipe
          swiping = false;

          return true;
        }

        swiping = false;
        break;
      }

      case MotionEvent.ACTION_CANCEL: {
        if (velocityTracker == null) {
          break;
        }
        Log.d("SWIPE", "CANCEL");
        if (downView != null) {
          // cancel
          animate(downView)
                  .translationX(0)
                  .alpha(1)
                  .setDuration(animationTime)
                  .setListener(null);
        }
        velocityTracker.recycle();
        velocityTracker = null;
        downX = 0;
        downY = 0;
        downView = null;
        downPosition = ListView.INVALID_POSITION;
        swiping = false;
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        if (velocityTracker == null || paused) {
          break;
        }

        velocityTracker.addMovement(motionEvent);
        float deltaX = motionEvent.getRawX() - downX;
        float deltaY = motionEvent.getRawY() - downY;
        if (Math.abs(deltaX) > slop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
          swiping = true;
          swipingSlop = (deltaX > 0 ? slop : -slop);
          listView.requestDisallowInterceptTouchEvent(true);

          // Cancel ListView's touch (un-highlighting the item)
          MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
          cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                  (motionEvent.getActionIndex()
                          << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
          listView.onTouchEvent(cancelEvent);
          view.onTouchEvent(cancelEvent);
          cancelEvent.recycle();
        }

        if (swiping) {
          setTranslationX(downView, deltaX - swipingSlop);
          setAlpha(downView, Math.max(0f, Math.min(1f,
                  1f - 2f * Math.abs(deltaX) / viewWidth)));

          return true;
        }
        break;
      }
    }
    return false;
  }

  private void dismiss(final View view, final int position, boolean dismissRight) {
    ++dismissAnimationRefCount;
    if (view == null) {
      // No view, shortcut to calling onDismiss to let it deal with adapter
      // updates and all that.
      callback.onDismiss(listView, new int[] { position });
      return;
    }

    animate(view)
            .translationX(dismissRight ? viewWidth : -viewWidth)
            .alpha(0)
            .setDuration(animationTime)
            .setListener(new AnimatorListenerAdapter() {

              @Override
              public void onAnimationEnd(Animator animation) {
                performDismiss(view, position);
              }
            });
  }

  class PendingDismissData implements Comparable<PendingDismissData> {

    public int position;
    public View view;

    public PendingDismissData(int position, View view) {
      this.position = position;
      this.view = view;
    }

    @Override
    public int compareTo(PendingDismissData other) {
      // Sort by descending position
      return other.position - position;
    }
  }

  private void performDismiss(final View dismissView, final int dismissPosition) {
    // Animate the dismissed list item to zero-height and fire the dismiss callback when
    // all dismissed list item animations have completed. This triggers layout on each animation
    // frame; in the future we may want to do something smarter and more performant.

    final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
    final int originalHeight = dismissView.getHeight();

    ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(animationTime);

    animator.addListener(new AnimatorListenerAdapter() {

      @Override
      public void onAnimationEnd(Animator animation) {
        --dismissAnimationRefCount;
        if (dismissAnimationRefCount == 0) {
          // No active animations, process all pending dismisses.
          // Sort by descending position
          Collections.sort(pendingDismisses);

          int[] dismissPositions = new int[pendingDismisses.size()];
          for (int i = pendingDismisses.size() - 1; i >= 0; i--) {
            dismissPositions[i] = pendingDismisses.get(i).position;
          }
          callback.onDismiss(listView, dismissPositions);

          // Reset mDownPosition to avoid MotionEvent.ACTION_UP trying to start a dismiss
          // animation with a stale position
          downPosition = ListView.INVALID_POSITION;

          ViewGroup.LayoutParams lp;
          for (PendingDismissData pendingDismiss : pendingDismisses) {
            // Reset view presentation
            setAlpha(pendingDismiss.view, 1f);
            setTranslationX(pendingDismiss.view, 0);
            lp = pendingDismiss.view.getLayoutParams();
            /*https://github.com/gabrielemariotti/cardslib/commit/c530cbedd81e9237c856cb4117ca5c803358c611#diff-0ea7eb52e116d1769da42bbbada1600fR389
            //lp.height = originalHeight; */
            lp.height = 0;
            pendingDismiss.view.setLayoutParams(lp);
          }

          // Send a cancel event
          long time = SystemClock.uptimeMillis();
          MotionEvent cancelEvent = MotionEvent.obtain(time, time,
                  MotionEvent.ACTION_CANCEL, 0, 0, 0);
          listView.dispatchTouchEvent(cancelEvent);

          pendingDismisses.clear();
        }
      }
    });

    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

      @Override
      public void onAnimationUpdate(ValueAnimator valueAnimator) {
        lp.height = (Integer) valueAnimator.getAnimatedValue();
        dismissView.setLayoutParams(lp);
      }
    });

    pendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
    animator.start();
  }
}
