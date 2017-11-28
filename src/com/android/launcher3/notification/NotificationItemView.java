/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.notification;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.anim.PropertyResetListener;
import com.android.launcher3.anim.RoundedRectRevealOutlineProvider;
import com.android.launcher3.graphics.IconPalette;
import com.android.launcher3.logging.UserEventDispatcher.LogContainerProvider;
import com.android.launcher3.popup.PopupItemView;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.Themes;
import com.google.android.apps.nexuslauncher.R;

import java.util.List;

/**
 * A {@link FrameLayout} that contains a header, main view and a footer.
 * The main view contains the icon and text (title + subtext) of the first notification.
 * The footer contains: A list of just the icons of all the notifications past the first one.
 * @see NotificationFooterLayout
 */
public class NotificationItemView extends PopupItemView implements LogContainerProvider {

    private static final Rect sTempRect = new Rect();

    private TextView mHeaderCount;
    private TextView mHeaderText;
    private NotificationMainView mMainView;
    private NotificationFooterLayout mFooter;
    private SwipeHelper mSwipeHelper;
    private boolean mAnimatingNextIcon;
    private int mNotificationHeaderTextColor = Notification.COLOR_DEFAULT;

    public NotificationItemView(Context context) {
        this(context, null, 0);
    }

    public NotificationItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeaderCount = (TextView) findViewById(R.id.notification_count);
        mHeaderText = (TextView) findViewById(R.id.notification_text);
        mMainView = (NotificationMainView) findViewById(R.id.main_view);
        mFooter = (NotificationFooterLayout) findViewById(R.id.footer);
        mSwipeHelper = new SwipeHelper(SwipeHelper.X, mMainView, getContext());
        mSwipeHelper.setDisableHardwareLayers(true);
    }

    public NotificationMainView getMainView() {
        return mMainView;
    }

    public int getHeightMinusFooter() {
        int footerHeight = mFooter.getParent() == null ? 0 : mFooter.getHeight();
        return getHeight() - footerHeight;
    }

    public Animator animateHeightRemoval(int heightToRemove, final boolean b) {
        final AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
        final Rect rect = new Rect(this.mPillRect);
        final Rect rect2 = new Rect(this.mPillRect);
        if (b) {
            rect2.top += heightToRemove;
        } else {
            rect2.bottom -= heightToRemove;
        }
        animatorSet.play(new RoundedRectRevealOutlineProvider(this.getBackgroundRadius(), this.getBackgroundRadius(), rect, rect2, this.mRoundedCorners).createRevealAnimator(this, false));
        final View viewById = this.findViewById(R.id.gutter_bottom);
        if (viewById != null && viewById.getVisibility() == View.VISIBLE) {
            final ObjectAnimator ofFloat = ObjectAnimator.ofFloat(viewById, NotificationItemView.TRANSLATION_Y, new float[] { -heightToRemove });
            ofFloat.addListener(new PropertyResetListener(NotificationItemView.TRANSLATION_Y, 0.0f));
            animatorSet.play(ofFloat);
        }
        return animatorSet;
    }

    public void updateHeader(int notificationCount, @Nullable IconPalette palette) {
        mHeaderCount.setText(notificationCount <= 1 ? "" : String.valueOf(notificationCount));
        if (palette != null) {
            if (mNotificationHeaderTextColor == Notification.COLOR_DEFAULT) {
                mNotificationHeaderTextColor =
                        IconPalette.resolveContrastColor(getContext(), palette.dominantColor, Themes.getAttrColor(getContext(), R.attr.popupColorPrimary));
            }
            mHeaderText.setTextColor(mNotificationHeaderTextColor);
            mHeaderCount.setTextColor(mNotificationHeaderTextColor);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mMainView.getNotificationInfo() == null) {
            // The notification hasn't been populated yet.
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        return mSwipeHelper.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mMainView.getNotificationInfo() == null) {
            // The notification hasn't been populated yet.
            return false;
        }
        return mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    public void applyNotificationInfos(final List<NotificationInfo> notificationInfos) {
        if (notificationInfos.isEmpty()) {
            return;
        }

        NotificationInfo mainNotification = notificationInfos.get(0);
        mMainView.applyNotificationInfo(mainNotification, mIconView);

        for (int i = 1; i < notificationInfos.size(); i++) {
            mFooter.addNotificationInfo(notificationInfos.get(i));
        }
        mFooter.commitNotificationInfos();
    }

    public void trimNotifications(final List<String> notificationKeys) {
        boolean dismissedMainNotification = !notificationKeys.contains(
                mMainView.getNotificationInfo().notificationKey);
        if (dismissedMainNotification && !mAnimatingNextIcon) {
            // Animate the next icon into place as the new main notification.
            mAnimatingNextIcon = true;
            mMainView.setVisibility(INVISIBLE);
            mMainView.setTranslationX(0);
            mIconView.getGlobalVisibleRect(sTempRect);
            mFooter.animateFirstNotificationTo(sTempRect,
                    new NotificationFooterLayout.IconAnimationEndListener() {
                @Override
                public void onIconAnimationEnd(NotificationInfo newMainNotification) {
                    if (newMainNotification != null) {
                        mMainView.applyNotificationInfo(newMainNotification, mIconView, true);
                        mMainView.setVisibility(VISIBLE);
                    }
                    mAnimatingNextIcon = false;
                }
            });
        } else {
            mFooter.trimNotifications(notificationKeys);
        }
    }

    @Override
    public void fillInLogContainerData(View v, ItemInfo info, LauncherLogProto.Target target,
            LauncherLogProto.Target targetParent) {
        target.itemType = LauncherLogProto.ItemType.NOTIFICATION;
        targetParent.containerType = LauncherLogProto.ContainerType.DEEPSHORTCUTS;
    }
}
