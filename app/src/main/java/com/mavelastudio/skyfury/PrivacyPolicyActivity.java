package com.mavelastudio.skyfury;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public final class PrivacyPolicyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        TextView policyText = findViewById(R.id.privacy_policy_text);
        Button openLinkButton = findViewById(R.id.privacy_policy_open_link);
        Button closeButton = findViewById(R.id.privacy_policy_close);

        policyText.setText(getString(R.string.privacy_policy_summary));
        openLinkButton.setOnClickListener(v -> {
            String url = getString(R.string.privacy_policy_url);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
        closeButton.setOnClickListener(v -> finish());
    }
}
