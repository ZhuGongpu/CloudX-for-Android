package cloudx.view.menu;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import cloudx.main.R;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * User: special
 * Date: 13-12-10
 * Time: 下午10:44
 * Mail: specialcyci@gmail.com
 */
public class ResideMenu extends FrameLayout {

    private ImageView iv_shadow;
    private ImageView iv_background;
    private LinearLayout layout_menu;
    private ScrollView sv_menu;
    private AnimatorSet scaleUp_shadow;
    private AnimatorSet scaleUp_activity;
    private AnimatorSet scaleDown_activity;
    private AnimatorSet scaleDown_shadow;
    /**
     * the activity that view attach to
     */
    private Activity activity;
    /**
     * the decorview of the activity
     */
    private ViewGroup view_decor;
    /**
     * the viewgroup of the activity
     */
    private ViewGroup view_activity;
    /**
     * the flag of menu open status
     */
    private boolean isOpened;

    private float shadow_ScaleX;
    /**
     * the view which don't want to intercept touch event
     */

    private List<ResideMenuItem> menuItems;
    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private OnMenuListener menuListener;
    private Animator.AnimatorListener animationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            if (isOpened) {
                layout_menu.removeAllViews();
                showMenuDelay();
                if (menuListener != null)
                    menuListener.openMenu();
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // reset the view;
            if (!isOpened) {
                view_decor.removeView(ResideMenu.this);
                view_decor.removeView(sv_menu);
                if (menuListener != null)
                    menuListener.closeMenu();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    public ResideMenu(Context context) {
        super(context);
        initViews(context);
    }

    private void initViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.residemenu, this);
        sv_menu = (ScrollView) findViewById(R.id.sv_menu);
        iv_shadow = (ImageView) findViewById(R.id.iv_shadow);
        layout_menu = (LinearLayout) findViewById(R.id.layout_menu);
        iv_background = (ImageView) findViewById(R.id.iv_background);
    }

    /**
     * use the method to set up the activity which resideMenu need to show;
     *
     * @param activity
     */
    public void attachToActivity(Activity activity) {
        initValue(activity);
        setShadowScaleXByOrientation();
        buildAnimationSet();
    }


    /**
     * 包括截图在内的一系列初始化
     *
     * @param activity
     */
    private void initValue(Activity activity) {
        this.activity = activity;
        menuItems = new ArrayList<ResideMenuItem>();

        //截图
        view_decor = (ViewGroup) activity.getWindow().getDecorView();

        view_activity = (ViewGroup) view_decor.getChildAt(0);
    }

    //todo need to modify
    private void setShadowScaleXByOrientation() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            shadow_ScaleX = 0.5335f;
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            shadow_ScaleX = 0.56f;
        }
    }

    /**
     * set the menu menu_background picture;
     *
     * @param imageResource
     */
    public void setBackground(int imageResource) {
        iv_background.setImageResource(imageResource);
    }

    /**
     * the visiblity of shadow under the activity view;
     *
     * @param isVisible
     */
    public void setShadowVisible(boolean isVisible) {
        if (isVisible)
            iv_shadow.setImageResource(R.drawable.shadow);
        else
            iv_shadow.setImageBitmap(null);
    }

    /**
     * add a single items;
     *
     * @param menuItem
     */
    public void addMenuItem(ResideMenuItem menuItem) {
        this.menuItems.add(menuItem);
    }

    public List<ResideMenuItem> getMenuItems() {
        return menuItems;
    }

    /**
     * set the menu items by array list;
     *
     * @param menuItems
     */
    public void setMenuItems(List<ResideMenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    public OnMenuListener getMenuListener() {
        return menuListener;
    }

    /**
     * if you need to do something on the action of closing or opening
     * menu, set the listener here.
     *
     * @return
     */
    public void setMenuListener(OnMenuListener menuListener) {
        this.menuListener = menuListener;
    }

    /**
     * we need the call the method before the menu show, because the
     * padding of activity can't get at the moment of onCreateView();
     */
    private void setViewPadding() {
        this.setPadding(view_activity.getPaddingLeft(),
                view_activity.getPaddingTop(),
                view_activity.getPaddingRight(),
                view_activity.getPaddingBottom());
    }

    /**
     * show the reside menu;
     */
    public void openMenu() {
        Log.e("", "openMenu    " + isOpened);

        if (!isOpened) {
            isOpened = true;
            showOpenMenuRelative();
        }
    }

    private void removeMenuLayout() {
        ViewGroup parent = ((ViewGroup) sv_menu.getParent());
        parent.removeView(sv_menu);
    }

    /**
     * close the reslide menu;
     */
    public void closeMenu() {

        Log.e("", "closeMenu    " + isOpened);

        if (isOpened) {
            isOpened = false;
            scaleUp_activity.start();
        }
    }

    /**
     * return the flag of menu status;
     *
     * @return
     */
    public boolean isOpened() {
        return isOpened;
    }

    /**
     * call the method relative to open menu;
     */
    private void showOpenMenuRelative() {
        setViewPadding();
        scaleDown_activity.start();
        // remove self if has not remove
        if (getParent() != null) view_decor.removeView(this);
        if (sv_menu.getParent() != null) removeMenuLayout();
        view_decor.addView(this, 0);
        view_decor.addView(sv_menu);
    }

    private void showMenuDelay() {
        layout_menu.removeAllViews();
        for (int i = 0; i < menuItems.size(); i++)
            showMenuItem(menuItems.get(i), i);
    }

    /**
     * @param menuItem
     * @param menu_index the position of the menu;
     * @return
     */
    private void showMenuItem(ResideMenuItem menuItem, int menu_index) {

        layout_menu.addView(menuItem);
        ViewHelper.setAlpha(menuItem, 0);
        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(
                ObjectAnimator.ofFloat(menuItem, "translationX", -100.f, 0.0f),
                ObjectAnimator.ofFloat(menuItem, "alpha", 0.0f, 1.0f)
        );

        scaleUp.setInterpolator(AnimationUtils.loadInterpolator(activity,
                android.R.anim.anticipate_overshoot_interpolator));
        // with animation;
        scaleUp.setStartDelay(50 * menu_index);
        scaleUp.setDuration(400).start();
    }

    private void buildAnimationSet() {

        scaleUp_activity = buildScaleUpAnimation(view_activity, 1.0f, 1.0f);
        scaleUp_shadow = buildScaleUpAnimation(iv_shadow, 1.0f, 1.0f);

        //todo changed
        scaleDown_activity = buildScaleDownAnimation(view_activity, 0.53f, 0.50f);
//        scaleDown_activity = buildScaleDownAnimation(view_activity, 0.75f, 0.7f);
        scaleDown_shadow = buildScaleDownAnimation(iv_shadow, shadow_ScaleX, 0.535f);
//        scaleDown_shadow = buildScaleDownAnimation(iv_shadow, 0.81f, 1.1f);


        scaleUp_activity.addListener(animationListener);
        scaleUp_activity.playTogether(scaleUp_shadow);
        scaleDown_shadow.addListener(animationListener);
        scaleDown_activity.playTogether(scaleDown_shadow);
    }

    /**
     * a helper method to build scale down animation;
     *
     * @param target
     * @param targetScaleX
     * @param targetScaleY
     * @return
     */
    private AnimatorSet buildScaleDownAnimation(View target, float targetScaleX, float targetScaleY) {

        // set the pivotX and pivotY to scale;
        int pivotX = (int) (getScreenWidth() * 1.5);
        int pivotY = (int) (getScreenHeight() * 0.5);

        ViewHelper.setPivotX(target, pivotX);
        ViewHelper.setPivotY(target, pivotY);
        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.playTogether(
                ObjectAnimator.ofFloat(target, "scaleX", targetScaleX),
                ObjectAnimator.ofFloat(target, "scaleY", targetScaleY)
        );

        scaleDown.setInterpolator(AnimationUtils.loadInterpolator(activity,
                android.R.anim.decelerate_interpolator));
        scaleDown.setDuration(250);
        return scaleDown;
    }

    /**
     * a helper method to build scale up animation;
     *
     * @param target
     * @param targetScaleX
     * @param targetScaleY
     * @return
     */
    private AnimatorSet buildScaleUpAnimation(View target, float targetScaleX, float targetScaleY) {

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(
                ObjectAnimator.ofFloat(target, "scaleX", targetScaleX),
                ObjectAnimator.ofFloat(target, "scaleY", targetScaleY)
        );

        scaleUp.setDuration(250);
        return scaleUp;
    }


    public int getScreenHeight() {
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    public int getScreenWidth() {
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public interface OnMenuListener {

        /**
         * the method will call on the finished time of opening menu's animation.
         */
        public void openMenu();

        /**
         * the method will call on the finished time of closing menu's animation  .
         */
        public void closeMenu();
    }

}
