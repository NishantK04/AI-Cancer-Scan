package com.nishant.cancerprediction;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatBottomSheet extends BottomSheetDialogFragment {

    private EditText etUserMessage;
    private Button btnSend;
    private ScrollView scrollView;
    private LinearLayout messagesContainer;

    private static final String FLASK_SERVER_URL = "http://10.10.143.77:5003/ask";

    private final OkHttpClient client = new OkHttpClient();
    private TextView typingIndicatorView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_chat, container, false);

        etUserMessage = view.findViewById(R.id.etUserMessage);
        btnSend = view.findViewById(R.id.btnSend);
        scrollView = view.findViewById(R.id.scrollViewBotReply);
        messagesContainer = view.findViewById(R.id.messagesContainer);

        ImageView arrowDown = view.findViewById(R.id.arrowDown);
        arrowDown.setOnClickListener(v -> dismiss());

        // Show greeting message from bot when chat opens
        addMessageToContainer("Hi! I’m Micky. How can I help you?", false);


        btnSend.setOnClickListener(v -> {
            String userMessage = etUserMessage.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                addMessageToContainer(userMessage, true); // user = true
                etUserMessage.setText("");
                typingIndicatorView = addMessageToContainer("Typing", false);
                startTypingAnimation();
                sendMessageToServer(userMessage);
            }
        });

        return view;
    }

    private TextView addMessageToContainer(String message, boolean isUser) {
        TextView textView = new TextView(getContext());
        textView.setText(message);
        textView.setTextSize(16);
        textView.setTextColor(isUser ? Color.WHITE : Color.BLACK);
        textView.setPadding(24, 16, 24, 16);

        int maxWidthPx = (int) (getResources().getDisplayMetrics().widthPixels * 0.7);
        textView.setMaxWidth(maxWidthPx);

        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setCornerRadius(24);
        bgDrawable.setColor(isUser ? Color.parseColor("#4CAF50") : Color.parseColor("#E0E0E0"));
        textView.setBackground(bgDrawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(12, 8, 12, 8);
        params.gravity = isUser ? Gravity.END : Gravity.START;
        textView.setLayoutParams(params);

        messagesContainer.addView(textView);
        scrollView.post(() -> scrollView.smoothScrollTo(0, messagesContainer.getBottom()));

        return textView;
    }

    private void sendMessageToServer(String message) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            json.put("message", message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_SERVER_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            final Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.post(() -> {
                    removeTypingIndicator();
                    addMessageToContainer("Error: " + e.getMessage(), false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String replyText = "Error";
                if (response.isSuccessful()) {
                    try {
                        String responseStr = response.body().string();
                        JSONObject responseJson = new JSONObject(responseStr);
                        replyText = responseJson.getString("reply");
                    } catch (Exception e) {
                        replyText = "Parsing error: " + e.getMessage();
                    }
                } else {
                    replyText = "Server error: " + response.message();
                }

                String finalReplyText = replyText;
                handler.post(() -> {
                    removeTypingIndicator();
                    addMessageToContainer(finalReplyText, false);
                });
            }
        });
    }

    private void removeTypingIndicator() {
        if (typingIndicatorView != null) {
            messagesContainer.removeView(typingIndicatorView);
            typingIndicatorView = null;
        }
    }

    private void startTypingAnimation() {
        Handler dotHandler = new Handler(Looper.getMainLooper());
        dotHandler.postDelayed(new Runnable() {
            int dotCount = 0;

            @Override
            public void run() {
                if (typingIndicatorView == null) return;
                dotCount = (dotCount % 3) + 1;
                String dots = new String(new char[dotCount]).replace("\0", ".");
                typingIndicatorView.setText("Typing" + dots);
                dotHandler.postDelayed(this, 400);
            }
        }, 400);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                int margin = (int) (getResources().getDisplayMetrics().density * 16);
                int screenWidth = getResources().getDisplayMetrics().widthPixels;

                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.width = screenWidth - (margin * 2);
                bottomSheet.setLayoutParams(layoutParams);

                bottomSheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setDraggable(false);
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });
        return dialog;
    }
}
