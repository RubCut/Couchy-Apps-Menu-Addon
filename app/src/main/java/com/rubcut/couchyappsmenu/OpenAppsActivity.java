package com.rubcut.couchyappsmenu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Separate entry-point activity for launching the Couchy Apps Menu.
 * Easily bound to the TV remote 9-dots button via Button Mapper,
 * custom TV launchers (like Couchy, Projectivy, ATV Launcher),
 * automation, and ADB shell commands.
 */
public final class OpenAppsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Forward any original intent extras or action if present
        Intent original = getIntent();
        if (original != null && original.getAction() != null) {
            intent.setAction(original.getAction());
        }

        startActivity(intent);
        finish();
    }
}
