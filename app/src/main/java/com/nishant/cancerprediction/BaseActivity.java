package com.nishant.cancerprediction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Let the child activity handle setContentView()
        // We just hook into it AFTER the layout is set
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Hook FAB after the layout is loaded
        FloatingActionButton aiFab = findViewById(R.id.ai_fab);
        if (aiFab != null) {
            aiFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Launch the AI Assistant Activity
                    ChatBottomSheet bottomSheet = new ChatBottomSheet();
                    bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
                }
            });
        }
    }
}