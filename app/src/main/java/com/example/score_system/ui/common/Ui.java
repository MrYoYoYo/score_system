package com.example.score_system.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public final class Ui {
    public static final int INK = Color.rgb(28, 42, 64);
    public static final int MUTED = Color.rgb(113, 128, 150);
    public static final int BLUE = Color.rgb(54, 101, 237);
    public static final int BACKGROUND = Color.rgb(245, 247, 252);
    public static final int GREEN = Color.rgb(23, 137, 111);

    private Ui() {}

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout page(AppCompatActivity activity) {
        LinearLayout root = column(activity);
        root.setBackgroundColor(BACKGROUND);
        activity.setContentView(root);
        insets(root);
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        LinearLayout body = column(activity);
        body.setPadding(dp(activity, 24), dp(activity, 24), dp(activity, 24), dp(activity, 24));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        return body;
    }

    public static void insets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    public static TextView text(Context context, String value, int size, int color) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setLineSpacing(dp(context, 3), 1);
        return text;
    }

    public static TextView heading(LinearLayout parent, String title, String subtitle) {
        Context context = parent.getContext();
        TextView label = text(context, title, 27, INK);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        parent.addView(label);
        if (!subtitle.isEmpty()) {
            TextView description = text(context, subtitle, 13, MUTED);
            description.setPadding(0, dp(context, 8), 0, dp(context, 16));
            parent.addView(description);
        }
        return label;
    }

    public static void section(LinearLayout parent, String title) {
        TextView label = text(parent.getContext(), title, 18, INK);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setPadding(0, dp(parent.getContext(), 24), 0, dp(parent.getContext(), 12));
        parent.addView(label);
    }

    public static LinearLayout card(LinearLayout parent, String title, String detail) {
        Context context = parent.getContext();
        MaterialCardView card = new MaterialCardView(context);
        card.setRadius(dp(context, 20));
        card.setCardBackgroundColor(Color.WHITE);
        card.setCardElevation(0);
        card.setStrokeWidth(dp(context, 1));
        card.setStrokeColor(Color.rgb(232, 237, 245));
        LinearLayout body = column(context);
        body.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 16));
        TextView label = text(context, title, 17, INK);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        body.addView(label);
        if (!detail.isEmpty()) {
            TextView description = text(context, detail, 13, MUTED);
            description.setPadding(0, dp(context, 8), 0, dp(context, 4));
            body.addView(description);
        }
        card.addView(body);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(context, 12);
        parent.addView(card, params);
        return body;
    }

    public static void hero(LinearLayout parent, String title, String detail) {
        Context context = parent.getContext();
        LinearLayout card = column(context);
        card.setPadding(dp(context, 24), dp(context, 26), dp(context, 24), dp(context, 26));
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(48, 87, 215), Color.rgb(90, 132, 245)});
        background.setCornerRadius(dp(context, 24));
        card.setBackground(background);
        TextView label = text(context, title, 24, Color.WHITE);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(label);
        TextView description = text(context, detail, 14, Color.rgb(225, 234, 255));
        description.setPadding(0, dp(context, 12), 0, 0);
        card.addView(description);
        parent.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    public static MaterialButton button(LinearLayout parent, String label, Runnable action) {
        return button(parent, label, false, action);
    }

    public static MaterialButton button(LinearLayout parent, String label, boolean outlined, Runnable action) {
        Context context = parent.getContext();
        MaterialButton button = new MaterialButton(context, null, outlined ? com.google.android.material.R.attr.materialButtonOutlinedStyle : com.google.android.material.R.attr.materialButtonStyle);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setCornerRadius(dp(context, 14));
        button.setMinHeight(dp(context, 48));
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(context, 6);
        parent.addView(button, params);
        return button;
    }

    public static EditText field(LinearLayout parent, String hint, String value, int inputType) {
        Context context = parent.getContext();
        com.google.android.material.textfield.TextInputLayout wrapper = new com.google.android.material.textfield.TextInputLayout(context, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        wrapper.setHint(hint);
        wrapper.setBoxCornerRadii(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14));
        com.google.android.material.textfield.TextInputEditText input = new com.google.android.material.textfield.TextInputEditText(context);
        input.setTextColor(INK);
        input.setTextSize(15);
        input.setInputType(inputType);
        input.setText(value);
        if ((inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD && (inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) wrapper.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        wrapper.addView(input, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(context, 12);
        parent.addView(wrapper, params);
        return input;
    }

    public static LinearLayout dialogBody(Context context) {
        LinearLayout body = column(context);
        body.setPadding(dp(context, 24), dp(context, 8), dp(context, 24), dp(context, 12));
        return body;
    }

    public static void showField(EditText field, boolean visible) {
        View container = field;
        while (!(container instanceof com.google.android.material.textfield.TextInputLayout) && container.getParent() instanceof View) container = (View) container.getParent();
        container.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static ScrollView scroll(LinearLayout body) {
        ScrollView scroll = new ScrollView(body.getContext());
        scroll.addView(body);
        return scroll;
    }
}
