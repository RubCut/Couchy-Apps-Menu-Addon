package com.rubcut.couchyappsmenu;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Android TV apps drawer matching the classic ATV 4-column Leanback overlay
 * with semi-transparent black background and remote-first D-pad navigation.
 */
public final class MainActivity extends Activity implements AppGridAdapter.Listener {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService catalogExecutor = Executors.newSingleThreadExecutor();

    private GridView appGrid;
    private AppGridAdapter adapter;
    private View loadingState;
    private View emptyState;
    private TextView loadingText;

    private View searchBar;
    private View rowApps;
    private View rowNetflix;
    private View rowTips;
    private View rowYouTube;
    private View btnGetApps;
    private View btnGetGames;

    private int scanGeneration;
    private long lastScanRequestAt;
    private String focusedComponent;
    private boolean receiverRegistered;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
        loadingText = findViewById(R.id.loading_text);

        searchBar = findViewById(R.id.search_bar);
        rowApps = findViewById(R.id.row_apps);
        rowNetflix = findViewById(R.id.row_netflix);
        rowTips = findViewById(R.id.row_tips);
        rowYouTube = findViewById(R.id.row_youtube);
        btnGetApps = findViewById(R.id.btn_get_apps);
        btnGetGames = findViewById(R.id.btn_get_games);

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
            }
        });

        setupActions();
        setupFocusAnimations();
        setupNavigation();

        if (savedInstanceState != null) {
            focusedComponent = savedInstanceState.getString("focused_component");
        }
        reloadApps(true);
    }

    private void setupActions() {
        searchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchVoiceSearch();
            }
        });

        rowApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appGrid.requestFocus();
            }
        });

        rowNetflix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPackageOrStore("com.netflix.ninja", "Netflix", "com.netflix.mediaclient");
            }
        });

        rowTips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                } catch (RuntimeException ignored) {
                }
            }
        });

        rowYouTube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPackageOrStore("com.google.android.youtube.tv", "YouTube", "com.google.android.youtube");
            }
        });

        btnGetApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPlayStore();
            }
        });

        btnGetGames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPlayGames();
            }
        });
    }

    private void setupFocusAnimations() {
        setupViewFocusAnim(searchBar, 1.03f, dp(4));
        setupViewFocusAnim(rowApps, 1.04f, dp(4));
        setupViewFocusAnim(rowNetflix, 1.04f, dp(4));
        setupViewFocusAnim(rowTips, 1.04f, dp(4));
        setupViewFocusAnim(rowYouTube, 1.04f, dp(4));
        setupViewFocusAnim(btnGetApps, 1.05f, dp(6));
        setupViewFocusAnim(btnGetGames, 1.05f, dp(6));
    }

    private void setupViewFocusAnim(final View view, final float scale, final float elevation) {
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.animate()
                        .scaleX(hasFocus ? scale : 1f)
                        .scaleY(hasFocus ? scale : 1f)
                        .translationZ(hasFocus ? elevation : 0f)
                        .setDuration(hasFocus ? 120 : 90)
                        .start();
            }
        });
    }

    private void setupNavigation() {
        appGrid.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                int pos = appGrid.getSelectedItemPosition();
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && pos >= 0 && pos < 4) {
                    if (pos <= 1) {
                        btnGetApps.requestFocus();
                    } else {
                        btnGetGames.requestFocus();
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && (pos % 4 == 0)) {
                    rowApps.requestFocus();
                    return true;
                }
                return false;
            }
        });

        btnGetApps.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    appGrid.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    searchBar.requestFocus();
                    return true;
                }
                return false;
            }
        });

        btnGetGames.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    appGrid.requestFocus();
                    return true;
                }
                return false;
            }
        });

        searchBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    rowApps.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    btnGetApps.requestFocus();
                    return true;
                }
                return false;
            }
        });

        View.OnKeyListener sidebarKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    appGrid.requestFocus();
                    return true;
                }
                return false;
            }
        };

        rowApps.setOnKeyListener(sidebarKeyListener);
        rowNetflix.setOnKeyListener(sidebarKeyListener);
        rowTips.setOnKeyListener(sidebarKeyListener);
        rowYouTube.setOnKeyListener(sidebarKeyListener);
    }

    private void launchVoiceSearch() {
        try {
            startActivity(new Intent(Intent.ACTION_ASSIST));
        } catch (Exception e1) {
            try {
                startActivity(new Intent("android.speech.action.WEB_SEARCH"));
            } catch (Exception e2) {
                try {
                    startActivity(new Intent(Intent.ACTION_VOICE_COMMAND));
                } catch (Exception e3) {
                    Toast.makeText(this, R.string.search_hint, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openPlayStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?c=apps"));
            intent.setPackage("com.android.vending");
            startActivity(intent);
        } catch (Exception e1) {
            try {
                Intent launch = getPackageManager().getLaunchIntentForPackage("com.android.vending");
                if (launch != null) {
                    startActivity(launch);
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps")));
                }
            } catch (Exception e2) {
                Toast.makeText(this, R.string.get_more_apps, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openPlayGames() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.google.android.play.games");
            if (launch != null) {
                startActivity(launch);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?c=apps&q=games"));
            intent.setPackage("com.android.vending");
            startActivity(intent);
        } catch (Exception e1) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/category/GAME")));
            } catch (Exception e2) {
                Toast.makeText(this, R.string.get_more_games, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchPackageOrStore(String primaryPackage, String label, String fallbackPackage) {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(primaryPackage);
        if (intent == null && fallbackPackage != null) {
            intent = pm.getLaunchIntentForPackage(fallbackPackage);
        }
        if (intent != null) {
            try {
                startActivity(intent);
                return;
            } catch (RuntimeException ignored) {
            }
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + primaryPackage)));
        } catch (RuntimeException e) {
            Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerPackageReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SystemClock.elapsedRealtime() - lastScanRequestAt > 1_500L) {
            reloadApps(false);
        }
    }

    @Override
    protected void onStop() {
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
                } else {
                    appGrid.requestFocus();
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

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
