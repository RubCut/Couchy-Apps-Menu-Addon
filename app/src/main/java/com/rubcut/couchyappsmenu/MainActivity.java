package com.rubcut.couchyappsmenu;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.rubcut.couchyappsmenu.data.AppCatalog;
import com.rubcut.couchyappsmenu.data.AppEntry;
import com.rubcut.couchyappsmenu.ui.AppGridAdapter;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A small full-screen all-apps view designed to feel at home beside Couchy.
 * It deliberately has no network, account, or accessibility-service dependency.
 */
public final class MainActivity extends Activity implements AppGridAdapter.Listener {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService catalogExecutor = Executors.newSingleThreadExecutor();
    private final Date clockDate = new Date();

    private GridView appGrid;
    private AppGridAdapter adapter;
    private View loadingState;
    private View emptyState;
    private TextView subtitle;
    private TextView loadingText;
    private TextView clock;
    private int scanGeneration;
    private long lastScanRequestAt;
    private String focusedComponent;
    private boolean receiverRegistered;

    private final Runnable clockUpdater = new Runnable() {
        @Override
        public void run() {
            updateClock();
            long now = System.currentTimeMillis();
            long untilNextMinute = 60_000L - (now % 60_000L) + 80L;
            mainHandler.postDelayed(this, untilNextMinute);
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Package broadcasts may arrive in a small burst; one serial scan is enough.
            reloadApps(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullscreen();
        setContentView(R.layout.activity_main);

        appGrid = findViewById(R.id.app_grid);
        loadingState = findViewById(R.id.loading_state);
        emptyState = findViewById(R.id.empty_state);
        subtitle = findViewById(R.id.subtitle);
        loadingText = findViewById(R.id.loading_text);
        clock = findViewById(R.id.clock);

        adapter = new AppGridAdapter(this, this);
        appGrid.setAdapter(adapter);
        appGrid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < adapter.getCount()) {
                    focusedComponent = adapter.getItem(position).component.flattenToString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Keep the last component: it makes a refresh feel stable.
            }
        });

        if (savedInstanceState != null) {
            focusedComponent = savedInstanceState.getString("focused_component");
        }
        reloadApps(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerPackageReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateClock();
        mainHandler.removeCallbacks(clockUpdater);
        mainHandler.post(clockUpdater);

        // Returning from an installer/settings page is a common way the app list changes.
        if (SystemClock.elapsedRealtime() - lastScanRequestAt > 1_500L) {
            reloadApps(false);
        }
    }

    @Override
    protected void onStop() {
        mainHandler.removeCallbacks(clockUpdater);
        unregisterPackageReceiver();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        scanGeneration++;
        catalogExecutor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        reloadApps(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("focused_component", focusedComponent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_MENU
                || keyCode == KeyEvent.KEYCODE_INFO
                || keyCode == KeyEvent.KEYCODE_APP_SWITCH
                || keyCode == KeyEvent.KEYCODE_ALL_APPS)
                && event.getRepeatCount() == 0) {
            reloadApps(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onAppClicked(AppEntry app) {
        try {
            Intent launch = new Intent(Intent.ACTION_MAIN)
                    .setComponent(app.component)
                    .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(launch);
        } catch (RuntimeException exception) {
            Toast.makeText(this, getString(R.string.unable_to_open, app.label), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onAppLongClicked(AppEntry app) {
        try {
            Intent details = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            details.setData(Uri.fromParts("package", app.packageName, null));
            startActivity(details);
            return true;
        } catch (RuntimeException exception) {
            Toast.makeText(this, getString(R.string.unable_to_open, getString(R.string.app_info)), Toast.LENGTH_SHORT)
                    .show();
            return true;
        }
    }

    private void reloadApps(final boolean initialLoad) {
        final int generation = ++scanGeneration;
        lastScanRequestAt = SystemClock.elapsedRealtime();

        if (initialLoad && adapter.getCount() == 0) {
            loadingText.setText(R.string.loading_apps);
            loadingState.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        } else {
            subtitle.setText(R.string.refreshing);
        }

        catalogExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<AppEntry> apps;
                try {
                    apps = AppCatalog.load(getApplicationContext());
                } catch (RuntimeException exception) {
                    apps = java.util.Collections.emptyList();
                }
                final List<AppEntry> result = apps;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || generation != scanGeneration) {
                            return;
                        }
                        showApps(result);
                    }
                });
            }
        });
    }

    private void showApps(List<AppEntry> apps) {
        adapter.submit(apps);
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
        subtitle.setText(getString(R.string.apps_count, apps.size()));

        if (apps.isEmpty()) {
            return;
        }

        int position = positionForComponent(apps, focusedComponent);
        appGrid.setSelection(position);
        final int selection = position;
        appGrid.post(new Runnable() {
            @Override
            public void run() {
                View focusedChild = appGrid.getChildAt(selection - appGrid.getFirstVisiblePosition());
                if (focusedChild != null) {
                    focusedChild.requestFocus();
                }
            }
        });
    }

    private int positionForComponent(List<AppEntry> apps, String component) {
        if (component != null) {
            for (int index = 0; index < apps.size(); index++) {
                if (component.equals(apps.get(index).component.flattenToString())) {
                    return index;
                }
            }
        }
        return 0;
    }

    private void updateClock() {
        clockDate.setTime(System.currentTimeMillis());
        clock.setText(DateFormat.getTimeFormat(this).format(clockDate));
    }

    private void makeFullscreen() {
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void registerPackageReceiver() {
        if (receiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packageReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void unregisterPackageReceiver() {
        if (!receiverRegistered) {
            return;
        }
        unregisterReceiver(packageReceiver);
        receiverRegistered = false;
    }
}
