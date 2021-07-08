package com.example.horus.utils.keyboard;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.example.horus.R;

/**
 * des:
 * author: lognyun
 * date: 2018/12/2 09:50
 *
 * 键盘监听的start方法需要在onResume之后调用
 */
public class KeyboardHeightProvider  extends PopupWindow {

    /** The tag for logging purposes */
    private final static String TAG = "KeyboardHeightProvider";

    /** The keyboard height observer */
    private KeyboardHeightObserver observer;

    /** The cached landscape height of the keyboard */
    private int keyboardLandscapeHeight;

    /** The cached portrait height of the keyboard */
    private int keyboardPortraitHeight;

    /** The view that is used to calculate the keyboard height */
    private View popupView;

    /** The parent view */
    private View parentView;

    /** The root activity that uses this KeyboardHeightProvider */
    private Activity activity;

    /**
     * Construct a new KeyboardHeightProvider
     *
     * @param activity The parent activity
     */
    public KeyboardHeightProvider(Activity activity) {
        super(activity);
        this.activity = activity;

        LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        this.popupView = inflator.inflate(R.layout.pop_keyborad_height, null, false);
        setContentView(popupView);

        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

        parentView = activity.findViewById(android.R.id.content);

        setWidth(0);
        setHeight(WindowManager.LayoutParams.MATCH_PARENT);

        popupView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (popupView != null) {
                    handleOnGlobalLayout();
                }
            }
        });
    }

    /**
     * Start the KeyboardHeightProvider, this must be called after the onResume of the Activity.
     * PopupWindows are not allowed to be registered before the onResume has finished
     * of the Activity.
     */
    public void start() {

        if (!isShowing() && parentView.getWindowToken() != null) {
            setBackgroundDrawable(new ColorDrawable(0));
            showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0);
        }
    }

    /**
     * Close the keyboard height provider,
     * this provider will not be used anymore.
     */
    public void close() {
        this.observer = null;
        dismiss();
    }

    /**
     * Set the keyboard height observer to this provider. The
     * observer will be notified when the keyboard height has changed.
     * For example when the keyboard is opened or closed.
     *
     * @param observer The observer to be added to this provider.
     */
    public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {
        this.observer = observer;
    }

    /**
     * Get the screen orientation
     *
     * @return the screen orientation
     */
    private int getScreenOrientation() {
        return activity.getResources().getConfiguration().orientation;
    }

    /**
     * Popup window itself is as big as the window of the Activity.
     * The keyboard can then be calculated by extracting the popup view bottom
     * from the activity window height.
     *
     * Edit by lognyun 2019/01/15 不确定会不会引入新的BUG
     */
    private void handleOnGlobalLayout() {

        // 这是原代码作者的方式 但这种方式无法应对华为的可滑动隐藏导航栏
//        Point screenSize = new Point();
//        activity.getWindowManager().getDefaultDisplay().getSize(screenSize);

        // 修改后的方式（获取的是真实的内容视窗高度）
        View content = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        int[] screenPos = new int[2];
        content.getLocationOnScreen(screenPos);
        int realHeight = screenPos[1] + content.getHeight();


        Rect rect = new Rect();
        popupView.getWindowVisibleDisplayFrame(rect);

        // REMIND, you may like to change this using the fullscreen size of the phone
        // and also using the status bar and navigation bar heights of the phone to calculate
        // the keyboard height. But this worked fine on a Nexus.
        int orientation = getScreenOrientation();
//        int keyboardHeight = screenSize.y - rect.bottom;
        int keyboardHeight = realHeight - rect.bottom;


        // 由于布局变动不同步，还是有几率出现 -120/120等情况，过滤掉先
        if (keyboardHeight < 0) {
            keyboardHeight = 0;
        }
        if (keyboardHeight > 0 && keyboardHeight * 10 / realHeight < 2) {
            Log.e(TAG, "Illegal KeyboardHeight:" + keyboardHeight);
            return;
        }


        if (keyboardHeight == 0) {
            notifyKeyboardHeightChanged(0, orientation);
        }
        else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.keyboardPortraitHeight = keyboardHeight;
            notifyKeyboardHeightChanged(keyboardPortraitHeight, orientation);
        }
        else {
            this.keyboardLandscapeHeight = keyboardHeight;
            notifyKeyboardHeightChanged(keyboardLandscapeHeight, orientation);
        }
    }

    /**
     *
     */
    private void notifyKeyboardHeightChanged(int height, int orientation) {
        if (observer != null) {
            observer.onKeyboardHeightChanged(height, orientation);
        }
    }
}