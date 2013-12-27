/*
 * ******************************************************************************
 *   Copyright (c) 2013 Vaclav Souhrada, Gabriele Mariotti.
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
package it.gmariotti.cardslib.library.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.nineoldandroids.view.ViewPropertyAnimator;
import com.nineoldandroids.view.animation.AnimatorProxy;


/**
 *
 * @author vsouhrada (v.souhrada@gmail.com)
 * @version 1.0.0
 * @see android.widget.LinearLayout
 * @since 1.0.0
 */
public class NineLinearLayout extends LinearLayout {

  protected final AnimatorProxy animatorProxy;

  public NineLinearLayout(Context context) {
    super(context);
    animatorProxy = AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : null;
  }

  public NineLinearLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    animatorProxy = AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : null;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public NineLinearLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    animatorProxy = AnimatorProxy.NEEDS_PROXY ? AnimatorProxy.wrap(this) : null;
  }

  @Override
  public void setVisibility(int visibility) {
    if (animatorProxy != null) {
      if (visibility == GONE) {
        clearAnimation();
      } else if (visibility == VISIBLE) {
        setAnimation(animatorProxy);
      }
    }

    super.setVisibility(visibility);
  }

  @Override
  public float getAlpha() {
    if (AnimatorProxy.NEEDS_PROXY) {
      return animatorProxy.getAlpha();
    } else {
      return super.getAlpha();
    }
  }

  @Override
  public void setAlpha(float alpha) {
    if (AnimatorProxy.NEEDS_PROXY) {
      animatorProxy.setAlpha(alpha);
    } else {
      super.setAlpha(alpha);
    }
  }

  @Override
  public float getTranslationY() {
    if (AnimatorProxy.NEEDS_PROXY) {
      return animatorProxy.getTranslationY();
    } else {
      return super.getTranslationY();
    }
  }

  @Override
  public void setTranslationY(float translationY) {
    if (AnimatorProxy.NEEDS_PROXY) {
      animatorProxy.setTranslationY(translationY);
    } else {
      super.setTranslationY(translationY);
    }
  }

  @Override
  public float getTranslationX() {
    if (AnimatorProxy.NEEDS_PROXY) {
      return animatorProxy.getTranslationX();
    } else {
      return super.getTranslationX();
    }
  }

  @Override
  public void setTranslationX(float translationX) {
    if (AnimatorProxy.NEEDS_PROXY) {
      animatorProxy.setTranslationX(translationX);
    } else {
      super.setTranslationX(translationX);
    }
  }

  public ViewPropertyAnimator supportAnimate() {
      return ViewPropertyAnimator.animate(this);
  }

}
