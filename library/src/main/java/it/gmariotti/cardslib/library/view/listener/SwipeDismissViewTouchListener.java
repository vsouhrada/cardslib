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

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.view.CardView;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;


/**
 * <p>
 * It is based on Jake Wharton & Roman Nurik code.
 * See this link for original code:<br>
 * <a href="https://github.com/JakeWharton/SwipeToDismissNOA">SwipeToDismissNOA</a> &
 * <a href="https://github.com/romannurik/Android-SwipeToDismiss">Android-SwipeToDismiss</a>
 * </p>
 * <p/>
 * It provides a SwipeDismissViewTouchListener for a single Card.
 * </p>
 * If you are using a list, see {@link SwipeDismissListViewTouchListener}
 * </p>
 *
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class SwipeDismissViewTouchListener implements View.OnTouchListener {

  // Cached ViewConfiguration and system-wide constant values
  private int slop;
  private int minFlingVelocity;
  private int maxFlingVelocity;

  private long animationTime;

  // Fixed properties
  private CardView cardView;

  private DismissCallbacks callback;

  private int viewWidth = 1; // 1 and not 0 to prevent dividing by zero

  // Transient properties
  private float downX;
  private boolean swiping;
  private Card token;
  private VelocityTracker velocityTracker;
  private float translationX;

  private boolean mPaused;

  /**
   * The callback interface used by {@link SwipeDismissViewTouchListener}
   * to inform its client about a successful dismissal of the view for which it was created.
   */
  public interface DismissCallbacks {

    /**
     * Called to determine whether the given position can be dismissed.
     */
    boolean canDismiss(Card card);

    /**
     * Called when the user has indicated they she would like to dismiss the view.
     *
     * @param cardView The originating {@link it.gmariotti.cardslib.library.view.CardView}.
     * @parma card                   Card
     */
    void onDismiss(CardView cardView, Card card);
  }


  /**
   * Constructs a new swipe-to-dismiss touch listener for the given view.
   *
   * @param cardView  The card view which should be dismissable.
   * @param callback The callback to trigger when the user has indicated that she
   */
  public SwipeDismissViewTouchListener(CardView cardView, Card card, DismissCallbacks callback) {
    ViewConfiguration vc = ViewConfiguration.get(cardView.getContext());
    slop = vc.getScaledTouchSlop();
    minFlingVelocity = vc.getScaledMinimumFlingVelocity();
    maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
    animationTime = cardView.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
    this.cardView = cardView;
    token = card;
    this.callback = callback;
  }

  /**
   * Enables or disables (pauses or resumes) watching for swipe-to-dismiss
   * gestures.
   *
   * @param enabled Whether or not to watch for gestures.
   */
  public void setEnabled(boolean enabled) {
    mPaused = !enabled;
  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    // offset because the view is translated during swipe
    motionEvent.offsetLocation(translationX, 0);

    if (viewWidth < 2) {
      viewWidth = cardView.getWidth();
    }

    switch (motionEvent.getActionMasked()) {
      case MotionEvent.ACTION_DOWN: {
        if (mPaused) {
          return false;
        }

        // TODO: ensure this is a finger, and set a flag
        downX = motionEvent.getRawX();
        if (callback.canDismiss(token)) {
          velocityTracker = VelocityTracker.obtain();
          velocityTracker.addMovement(motionEvent);
        }

        view.onTouchEvent(motionEvent);
        return true;
        //return false;  fixing swipe and click together
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
        if (Math.abs(deltaX) > viewWidth / 2) {
          dismiss = true;
          dismissRight = deltaX > 0;
        } else if (minFlingVelocity <= velocityX && velocityX <= maxFlingVelocity
                && velocityY < velocityX) {
          dismiss = true;
          dismissRight = velocityTracker.getXVelocity() > 0;
        }
        if (dismiss) {
          // dismiss
          animate(cardView)
                  .translationX(dismissRight ? viewWidth : -viewWidth)
                  .alpha(0)
                  .setDuration(animationTime)
                  .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                      performDismiss();
                    }
                  });
        } else {
          // cancel
          animate(cardView)
                  .translationX(0)
                  .alpha(1)
                  .setDuration(animationTime)
                  .setListener(null);
        }
        velocityTracker = null;
        translationX = 0;
        downX = 0;
        swiping = false;
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        if (velocityTracker == null) {
          break;
        }

        velocityTracker.addMovement(motionEvent);
        float deltaX = motionEvent.getRawX() - downX;
        if (Math.abs(deltaX) > slop) {
          swiping = true;
          cardView.getParent().requestDisallowInterceptTouchEvent(true);

          // Cancel listview's touch
          MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
          cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                  (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
          cardView.onTouchEvent(cancelEvent);
          cancelEvent.recycle();
        }

        if (swiping) {
          translationX = deltaX;
          cardView.setTranslationX(deltaX);
          // TODO: use an ease-out interpolator or such
          cardView.setAlpha(Math.max(0f, Math.min(1f, 1f - 2f * Math.abs(deltaX) / viewWidth)));
          return true;
        }
        break;
      }
    }
    return false;
  }

  private void performDismiss() {
    // Animate the dismissed view to zero-height and then fire the dismiss callback.
    // This triggers layout on each animation frame; in the future we may want to do something
    // smarter and more performant.

    final ViewGroup.LayoutParams lp = cardView.getLayoutParams();
    final int originalHeight = cardView.getHeight();

    ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(animationTime);

    animator.addListener(new AnimatorListenerAdapter() {

      @Override
      public void onAnimationEnd(Animator animation) {
        callback.onDismiss(cardView, token);
        // Reset view presentation
        cardView.setAlpha(1f);
        cardView.setTranslationX(0);
        lp.height = originalHeight;
        cardView.setLayoutParams(lp);
      }
    });

    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

      @Override
      public void onAnimationUpdate(ValueAnimator valueAnimator) {
        lp.height = (Integer) valueAnimator.getAnimatedValue();
        cardView.setLayoutParams(lp);
      }
    });

    animator.start();
  }
}
