package com.molvix.android.ui.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.molvix.android.R;
import com.molvix.android.ui.viewmodels.SearchViewModel;
import com.molvix.android.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MolvixSearchView extends FrameLayout {


    @BindView(R.id.search_box_outer_container)
    View searchBoxOuterContainer;

    @BindView(R.id.search_box)
    EditText searchBox;

    @BindView(R.id.close_search)
    ImageView closeSearchView;

    public MolvixSearchView(@NonNull Context context) {
        super(context);
        initUI(context);
    }

    public MolvixSearchView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initUI(context);
    }

    public MolvixSearchView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initUI(context);
    }

    private void initUI(Context context) {
        @SuppressLint("InflateParams") View searchView = LayoutInflater.from(context).inflate(R.layout.layout_search_view, null);
        ButterKnife.bind(this, searchView);
        removeAllViews();
        addView(searchView);
        requestLayout();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setup() {
        VectorDrawableCompat searchIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_search_fair_white_24dp, null);
        searchBox.setCompoundDrawablesWithIntrinsicBounds(searchIcon, null, null, null);
        searchBox.setOnClickListener(v -> searchBox.setCursorVisible(true));
        searchBox.setOnTouchListener((v, event) -> {
            if (!searchBox.isCursorVisible()) {
                searchBox.setCursorVisible(true);
            }
            return false;
        });
        searchBoxOuterContainer.setOnClickListener(v -> searchBox.performClick());
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
                String searchedString = s.toString();
                SearchViewModel searchViewModel = new SearchViewModel();
                searchViewModel.updateSearch(searchedString);
                UiUtils.toggleViewVisibility(closeSearchView, StringUtils.isNotEmpty(searchedString));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
        closeSearchView.setOnClickListener(v -> searchBox.setText(""));
    }

    public void setText(String s) {
        searchBox.setText(s);
    }

    public String getText() {
        return searchBox.getText().toString().trim();
    }

}
