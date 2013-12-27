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


/**
 * @author vsouhrada (v.souhrada@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 * @see it.gmariotti.cardslib.library.view.NineLinearLayout
 */
public class UndoLayout extends NineLinearLayout {

  public UndoLayout(Context context) {
    super(context);
  }

  public UndoLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public UndoLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);
  }
}
