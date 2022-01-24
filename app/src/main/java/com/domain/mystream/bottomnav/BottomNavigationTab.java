package com.domain.mystream.bottomnav;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.domain.mystream.R;

/**
 * @author cubycode
 * @since 07/10/2018
 * All rights reserved
 */
enum BottomNavigationTab {

    STREAMS(R.string.bottom_menu_streams_tab, R.drawable.tab_home),
    SEARCH(R.string.bottom_menu_search_tab, R.drawable.tab_search),
    FOLLOWING(R.string.bottom_menu_following_tab, R.drawable.tab_following),
    ACCOUNT(R.string.bottom_menu_account_tab, R.drawable.tab_account),
    MESSAGES(R.string.bottom_menu_messaging_tab, R.drawable.tab_messages);

    private @StringRes
    int menuTitleRes;
    private @DrawableRes
    int iconRes;

    BottomNavigationTab(int menuTitleRes, int iconRes) {
        this.menuTitleRes = menuTitleRes;
        this.iconRes = iconRes;
    }

    public int getMenuTitleRes() {
        return menuTitleRes;
    }

    public void setMenuTitleRes(int menuTitleRes) {
        this.menuTitleRes = menuTitleRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }
}
