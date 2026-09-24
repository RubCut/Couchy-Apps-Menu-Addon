package com.rubcut.couchyappsmenu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.rubcut.couchyappsmenu.data.AppCatalog;
import com.rubcut.couchyappsmenu.data.AppEntry;
import com.rubcut.couchyappsmenu.data.HiddenAppsManager;
import com.rubcut.couchyappsmenu.ui.AppGridAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Android TV Side Apps Drawer.
 * Slides in from the right edge with strongly darkened semi-transparent black surface.
 * The left space is completely transparent and dismisses the drawer on click.
 * Features 20dp rounded 16:9 cards, robust focus navigation, and app hiding.
 */
public final class MainActivity extends Activity implements AppGridAdapter.Listener {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService catalogExecutor = Executors.newSingleThreadExecutor();

    private View rightPanel;
    private View dismissArea;
    private GridView appGrid;
    private AppGridAdapter adapter;
    private View loadingState;
    private View emptyState;
    private TextView loadingText;

    private View btnHiddenApps;
    private View btnGetApps;
    private View btnGetGames;

    private List<AppEntry> allLoadedApps = Collections.emptyList();
    private List<AppEntry> currentVisibleApps = Collections.emptyList();
    private List<AppEntry> currentHiddenApps = Collections.emptyList();

    private int scanGeneration;
    private long lastScanRequestAt;
    private String focusedComponent;
    private boolean receiverRegistered;
    private boolean isClosing;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadApps(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        makeFullscreen();
        setContentView(R.layout.activity_main);

        rightPanel = findViewById(R.id.right_panel);
        dismissArea = findViewById(R.id.dismiss_area);
        appGrid = findViewById(R.id.app_grid);
        loadingState = findViewById(R.id.loading_state);
        emptyState = findViewById(R.id.empty_state);
        loadingText = findViewById(R.id.loading_text);

        btnHiddenApps = findViewById(R.id.btn_hidden_apps);
        btnGetApps = findViewById(R.id.btn_get_apps);
        btnGetGames = findViewById(R.id.btn_get_games);

        adapter = new AppGridAdapter(this, this);
        appGrid.setAdapter(adapter);
        appGrid.setItemsCanFocus(true);

        appGrid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < adapter.getCount()) {
                    focusedComponent = adapter.getItem(position).component.flattenToString();
                    if (view != null) {
                        view.setSelected(true);
                        view.requestFocus();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setupActions();
        setupFocusAnimations();
        setupNavigation();
        animateOpen();

        if (savedInstanceState != null) {
            focusedComponent = savedInstanceState.getString("focused_component");
        }
        reloadApps(true);
    }

    private void animateOpen() {
        rightPanel.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rightPanel.getViewTreeObserver().removeOnPreDrawListener(this);
                int width = rightPanel.getWidth();
                if (width <= 0) {
                    width = Math.round(getResources().getDisplayMetrics().widthPixels * 0.67f);
                }
                rightPanel.setTranslationX(width);
                rightPanel.animate()
                        .translationX(0f)
                        .setDuration(280)
                        .setInterpolator(new DecelerateInterpolator(1.8f))
                        .start();
                return true;
            }
        });
    }

    private void animateClose() {
        if (isClosing) {
            return;
        }
        isClosing = true;
        int width = rightPanel.getWidth();
        if (width <= 0) {
            width = Math.round(getResources().getDisplayMetrics().widthPixels * 0.67f);
        }
        rightPanel.animate()
                .translationX(width)
                .setDuration(220)
                .setInterpolator(new AccelerateInterpolator(1.8f))
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.super.finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
    }

    @Override
    public void onBackPressed() {
        animateClose();
    }

    private void setupActions() {
        dismissArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateClose();
            }
        });

        btnHiddenApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHiddenAppsDialog();
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
        setupPillFocusAnim(btnHiddenApps, 1.1f);
        setupPillFocusAnim(btnGetApps, 1.05f);
        setupPillFocusAnim(btnGetGames, 1.05f);
    }

    private void setupPillFocusAnim(final View view, final float scale) {
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.animate()
                        .scaleX(hasFocus ? scale : 1f)
                        .scaleY(hasFocus ? scale : 1f)
                        .translationZ(hasFocus ? dp(6) : 0f)
                        .setDuration(hasFocus ? 120 : 90)
                        .start();
            }
        });
    }

    private void setupNavigation() {
        // App grid navigation
        appGrid.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                int pos = appGrid.getSelectedItemPosition();

                // Move up from row 0 to top buttons
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && pos >= 0 && pos < 4) {
                    if (pos == 0) {
                        btnHiddenApps.requestFocus();
                    } else if (pos <= 2) {
                        btnGetApps.requestFocus();
                    } else {
                        btnGetGames.requestFocus();
                    }
                    return true;
                }

                // Prevent cursor from escaping/disappearing when pressing left at column 0
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && (pos % 4 == 0)) {
                    return true;
                }

                return false;
            }
        });

        // Top button: Hidden Apps
        btnHiddenApps.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusGridFromTop(0);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    btnGetApps.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    return true; // Keep focus on the button
                }
                return false;
            }
        });

        // Top button: Get Apps
        btnGetApps.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusGridFromTop(1);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    btnHiddenApps.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    btnGetGames.requestFocus();
                    return true;
                }
                return false;
            }
        });

        // Top button: Get Games
        btnGetGames.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusGridFromTop(3);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    btnGetApps.requestFocus();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return true; // Keep focus on the button
                }
                return false;
            }
        });
    }

    private void focusGridFromTop(final int col) {
        if (adapter.getCount() == 0) {
            return;
        }
        final int targetPos = Math.min(col, adapter.getCount() - 1);
        appGrid.requestFocus();
        appGrid.setSelection(targetPos);
        appGrid.post(new Runnable() {
            @Override
            public void run() {
                int first = appGrid.getFirstVisiblePosition();
                View child = appGrid.getChildAt(targetPos - first);
                if (child != null) {
                    child.requestFocus();
                    child.setSelected(true);
                }
            }
        });
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
        // Toggle on 9-dots button (KEYCODE_ALL_APPS)
        if (keyCode == KeyEvent.KEYCODE_ALL_APPS && event.getRepeatCount() == 0) {
            animateClose();
            return true;
        }

        if ((keyCode == KeyEvent.KEYCODE_MENU
                || keyCode == KeyEvent.KEYCODE_INFO
                || keyCode == KeyEvent.KEYCODE_APP_SWITCH)
                && event.getRepeatCount() == 0) {
            showMenuDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showMenuDialog() {
        List<CharSequence> items = new ArrayList<>();
        items.add(getString(R.string.refreshing));
        items.add(getString(R.string.hidden_apps_title) + (!currentHiddenApps.isEmpty() ? " (" + currentHiddenApps.size() + ")" : ""));

        final CharSequence[] array = items.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.app_name)
                .setItems(array, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            reloadApps(false);
                        } else if (which == 1) {
                            showHiddenAppsDialog();
                        }
                    }
                })
                .setNegativeButton(R.string.close, null)
                .show();
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
    public boolean onAppLongClicked(final AppEntry app) {
        showAppOptionsDialog(app);
        return true;
    }

    private void showAppOptionsDialog(final AppEntry app) {
        CharSequence[] options = new CharSequence[]{
                getString(R.string.hide_app),
                getString(R.string.app_info),
                getString(R.string.open_app)
        };

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(app.label)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            hideApp(app);
                        } else if (which == 1) {
                            openAppDetails(app);
                        } else if (which == 2) {
                            onAppClicked(app);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void hideApp(AppEntry app) {
        HiddenAppsManager.setHidden(this, app.component.flattenToString(), true);
        Toast.makeText(this, getString(R.string.app_hidden_toast, app.label), Toast.LENGTH_SHORT).show();
        filterAndApplyApps();
    }

    private void showHiddenAppsDialog() {
        if (currentHiddenApps.isEmpty()) {
            Toast.makeText(this, R.string.no_hidden_apps, Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] labels = new CharSequence[currentHiddenApps.size()];
        for (int i = 0; i < currentHiddenApps.size(); i++) {
            labels[i] = currentHiddenApps.get(i).label;
        }

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(getString(R.string.hidden_apps_title))
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which >= 0 && which < currentHiddenApps.size()) {
                            showHiddenAppActionDialog(currentHiddenApps.get(which));
                        }
                    }
                })
                .setNeutralButton(R.string.restore_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HiddenAppsManager.unhideAll(MainActivity.this);
                        Toast.makeText(MainActivity.this, R.string.all_apps_restored, Toast.LENGTH_SHORT).show();
                        filterAndApplyApps();
                    }
                })
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void showHiddenAppActionDialog(final AppEntry app) {
        CharSequence[] options = new CharSequence[]{
                getString(R.string.unhide_app),
                getString(R.string.open_app),
                getString(R.string.app_info)
        };

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(app.label)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            HiddenAppsManager.setHidden(MainActivity.this, app.component.flattenToString(), false);
                            Toast.makeText(MainActivity.this, getString(R.string.app_unhidden_toast, app.label), Toast.LENGTH_SHORT).show();
                            filterAndApplyApps();
                        } else if (which == 1) {
                            onAppClicked(app);
                        } else if (which == 2) {
                            openAppDetails(app);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openAppDetails(AppEntry app) {
        try {
            Intent details = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            details.setData(Uri.fromParts("package", app.packageName, null));
            startActivity(details);
        } catch (RuntimeException exception) {
            Toast.makeText(this, getString(R.string.unable_to_open, getString(R.string.app_info)), Toast.LENGTH_SHORT).show();
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
                    apps = Collections.emptyList();
                }
                final List<AppEntry> result = apps;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || generation != scanGeneration) {
                            return;
                        }
                        allLoadedApps = result;
                        filterAndApplyApps();
                    }
                });
            }
        });
    }

    private void filterAndApplyApps() {
        Set<String> hiddenSet = HiddenAppsManager.getHidden(this);
        List<AppEntry> visible = new ArrayList<>();
        List<AppEntry> hidden = new ArrayList<>();

        for (AppEntry app : allLoadedApps) {
            if (hiddenSet.contains(app.component.flattenToString())) {
                hidden.add(app);
            } else {
                visible.add(app);
            }
        }

        currentVisibleApps = visible;
        currentHiddenApps = hidden;

        showApps(visible);
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
                    focusedChild.setSelected(true);
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
