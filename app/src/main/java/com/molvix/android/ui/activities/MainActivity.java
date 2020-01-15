package com.molvix.android.ui.activities;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.molvix.android.R;
import com.molvix.android.eventbuses.SearchEvent;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {

    @BindView(R.id.search_box_outter_container)
    View searchBoxOutterContainer;

    @BindView(R.id.search_box)
    EditText searchBox;

    @BindView(R.id.close_search)
    ImageView closeSearchView;

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

    @SuppressLint("ClickableViewAccessibility")
    private void initEventHandlers() {
        searchBox.setOnClickListener(v -> searchBox.setCursorVisible(true));
        searchBox.setOnTouchListener((v, event) -> {
            if (!searchBox.isCursorVisible()) {
                searchBox.setCursorVisible(true);
            }
            return false;
        });
        searchBoxOutterContainer.setOnClickListener(v -> searchBox.performClick());
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
                EventBus.getDefault().post(new SearchEvent(searchedString));
                UiUtils.toggleViewVisibility(closeSearchView, StringUtils.isNotEmpty(searchedString));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
        closeSearchView.setOnClickListener(v -> searchBox.setText(""));
    }

    @Override
    public void onBackPressed() {
        String searchString = searchBox.getText().toString().trim();
        if (StringUtils.isNotEmpty(searchString)) {
            searchBox.setText("");
        } else {
            super.onBackPressed();
        }
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
