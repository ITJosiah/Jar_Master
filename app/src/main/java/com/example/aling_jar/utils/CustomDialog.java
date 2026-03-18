package com.example.aling_jar.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.aling_jar.R;

public class CustomDialog {

    public enum Type {
        SUCCESS, WARNING, ERROR
    }

    public static class Builder {
        private final Context context;
        private Type type = Type.SUCCESS;
        private String title = "";
        private String message = "";
        private String primaryButtonText = "OK";
        private String secondaryButtonText = "";
        private View.OnClickListener primaryListener;
        private View.OnClickListener secondaryListener;
        private boolean cancelable = true;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setType(Type type) {
            this.type = type;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setPrimaryButton(String text, View.OnClickListener listener) {
            this.primaryButtonText = text;
            this.primaryListener = listener;
            return this;
        }

        public Builder setSecondaryButton(String text, View.OnClickListener listener) {
            this.secondaryButtonText = text;
            this.secondaryListener = listener;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Dialog build() {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_custom);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int)(context.getResources().getDisplayMetrics().widthPixels * 0.85),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.setCancelable(cancelable);

            ImageView ivIcon = dialog.findViewById(R.id.ivDialogIcon);
            TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
            TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
            Button btnPrimary = dialog.findViewById(R.id.btnDialogPrimary);
            Button btnSecondary = dialog.findViewById(R.id.btnDialogSecondary);

            // Set icon and colors based on type
            switch (type) {
                case SUCCESS:
                    ivIcon.setImageResource(R.drawable.ic_success);
                    ivIcon.setBackgroundResource(R.drawable.bg_dialog_icon_success);
                    btnPrimary.setBackgroundResource(R.drawable.bg_button_green);
                    btnPrimary.setTextColor(Color.parseColor("#22C55E"));
                    break;
                case WARNING:
                    ivIcon.setImageResource(R.drawable.ic_warning);
                    ivIcon.setBackgroundResource(R.drawable.bg_dialog_icon_warning);
                    btnPrimary.setBackgroundColor(Color.parseColor("#F59E0B"));
                    btnPrimary.setTextColor(Color.WHITE);
                    break;
                case ERROR:
                    ivIcon.setImageResource(R.drawable.ic_error);
                    ivIcon.setBackgroundResource(R.drawable.bg_dialog_icon_error);
                    btnPrimary.setBackgroundColor(Color.parseColor("#EF4444"));
                    btnPrimary.setTextColor(Color.WHITE);
                    break;
            }

            tvTitle.setText(title);
            tvMessage.setText(message);
            btnPrimary.setText(primaryButtonText);

            btnPrimary.setOnClickListener(v -> {
                if (primaryListener != null) primaryListener.onClick(v);
                dialog.dismiss();
            });

            if (!secondaryButtonText.isEmpty()) {
                btnSecondary.setVisibility(View.VISIBLE);
                btnSecondary.setText(secondaryButtonText);
                btnSecondary.setOnClickListener(v -> {
                    if (secondaryListener != null) secondaryListener.onClick(v);
                    dialog.dismiss();
                });
            }

            return dialog;
        }

        public void show() {
            build().show();
        }
    }
}