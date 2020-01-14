package com.molvix.android.ui.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.molvix.android.R;
import com.molvix.android.companions.AppConstants;
import com.molvix.android.eventbuses.SearchEvent;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {

    @BindView(R.id.search_box)
    EditText searchBox;

    @BindView(R.id.bottom_navigation_view)
    BottomNavigationView bottomNavView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initSearchBox();
        initNavBarTints();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(bottomNavView, navController);
        initEventHandlers();
    }

    private void initEventHandlers() {
        searchBox.setOnClickListener(v -> searchBox.setCursorVisible(true));
        searchBox.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                searchBox.setCursorVisible(false);
            }
        });
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchedString = StringUtils.strip(s.toString().trim());
                if (StringUtils.isNotEmpty(searchedString)) {
                    EventBus.getDefault().post(new SearchEvent(searchedString));
                } else {
                    EventBus.getDefault().post(AppConstants.EMPTY_SEARCH);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initSearchBox() {
        VectorDrawableCompat searchIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_search_fair_white_24dp, null);
        searchBox.setCompoundDrawablesWithIntrinsicBounds(searchIcon, null, null, null);
    }

    private void initNavBarTints() {
        ColorStateList iconsColorStates = new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
                new int[]{ContextCompat.getColor(this, R.color.grey500), Color.BLACK});

        ColorStateList textColorStates = new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}},
                new int[]{ContextCompat.getColor(this, R.color.grey500), Color.BLACK});
        bottomNavView.setItemIconTintList(iconsColorStates);
        bottomNavView.setItemTextColor(textColorStates);
    }

}
