package com.nishant.cancerprediction;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    private TextView tvBotReply;
    private Button btnSend;
    private ScrollView scrollView;

    // Replace with your Flask backend URL
    private static final String FLASK_SERVER_URL = "http://192.168.0.190:5003/ask";

    private final OkHttpClient client = new OkHttpClient();

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
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_chat, container, false);

        etUserMessage = view.findViewById(R.id.etUserMessage);
        tvBotReply = view.findViewById(R.id.tvBotReply);
        btnSend = view.findViewById(R.id.btnSend);
        scrollView = view.findViewById(R.id.scrollViewBotReply);

        btnSend.setOnClickListener(v -> {
            String userMessage = etUserMessage.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                sendMessageToServer(userMessage);
            }
        });

        return view;
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
                handler.post(() -> tvBotReply.setText("Error: " + e.getMessage()));
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
                handler.post(() -> tvBotReply.setText(finalReplyText));
            }
        });
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
