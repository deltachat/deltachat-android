package org.thoughtcrime.securesms.deltax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.b44t.messenger.DcContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.SearchToolbar;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.deltax.module.PluginInfo;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.ViewUtil;

public class DeltaXActivity extends BaseActionBarActivity
    implements PluginAdapter.PluginActionListener {

  private enum SearchField {
    NAME,
    AUTHOR,
    VERSION,
    PACKAGE
  }

  private DeltaX deltaX;
  private RecyclerView recycler;
  private TextView emptyView;
  private PluginAdapter adapter;
  private ActivityResultLauncher<Intent> pickerLauncher;

  private boolean selectionActive = false;
  private final Set<String> selectedIds = new HashSet<>();
  private ActionMode actionMode;
  private TextView title;
  private SearchToolbar searchToolbar;
  private ImageView searchAction;
  private SearchField currentField = SearchField.NAME;
  private String currentQuery = "";

  @Override
  protected void onPreCreate() {
    dynamicTheme = new DynamicNoActionBarTheme();
    super.onPreCreate();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_deltax);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    title = findViewById(R.id.toolbar_title);
    searchToolbar = findViewById(R.id.search_toolbar);
    searchAction = findViewById(R.id.search_action);
    ActionBar ab = getSupportActionBar();
    if (ab != null) {
      ab.setDisplayHomeAsUpEnabled(true);
    }
    title.setText(R.string.deltax_title);

    deltaX = DeltaX.getInstance(this);
    if (!deltaX.isInitialised()) {
      deltaX.init();
    }

    recycler = findViewById(R.id.deltax_recycler);
    recycler.setLayoutManager(new LinearLayoutManager(this));
    emptyView = findViewById(R.id.deltax_empty);

    View fab = findViewById(R.id.deltax_fab);
    fab.setOnClickListener(v -> openFilePicker());
    ViewCompat.setOnApplyWindowInsetsListener(
        fab,
        (v, insets) -> {
          Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
          ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
          int base = (int) (16 * v.getResources().getDisplayMetrics().density);
          lp.bottomMargin = base + nav.bottom;
          lp.leftMargin = base + nav.left;
          lp.rightMargin = base + nav.right;
          v.setLayoutParams(lp);
          return insets;
        });

    TooltipCompat.setTooltipText(searchAction, getText(R.string.search));

    searchAction.setOnClickListener(
        v ->
            searchToolbar.display(
                searchAction.getX() + (searchAction.getWidth() / 2),
                searchAction.getY() + (searchAction.getHeight() / 2)));

    ViewUtil.applyWindowInsetsAsMargin(searchToolbar, true, true, true, false);
    initializeFieldSelection();

    searchToolbar.setListener(
        new SearchToolbar.SearchListener() {
          @Override
          public void onSearchTextChange(String text) {
            currentQuery = text == null ? "" : text;
            refresh();
          }

          @Override
          public void onSearchClosed() {
            currentQuery = "";
            refresh();
          }
        });

    pickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == RESULT_OK
                  && result.getData() != null
                  && result.getData().getData() != null) {
                installFromUri(result.getData().getData());
              }
            });

    refresh();
  }

  private void initializeFieldSelection() {
    Toolbar inner = searchToolbar.findViewById(R.id.toolbar);
    if (inner == null) return;
    inner.getMenu().removeItem(R.id.search_unread);
    inner.inflateMenu(R.menu.deltax_search_field);
    inner.setOnMenuItemClickListener(
        item -> {
          int id = item.getItemId();
          if (id == R.id.search_field_name) currentField = SearchField.NAME;
          else if (id == R.id.search_field_author) currentField = SearchField.AUTHOR;
          else if (id == R.id.search_field_version) currentField = SearchField.VERSION;
          else if (id == R.id.search_field_package) currentField = SearchField.PACKAGE;
          else return false;
          updateSearchHint();
          if (!currentQuery.trim().isEmpty()) refresh();
          return true;
        });
  }

  private void updateSearchHint() {
    if (!searchToolbar.isVisible()) return;
    Toolbar inner = searchToolbar.findViewById(R.id.toolbar);
    if (inner == null) return;
    MenuItem item = inner.getMenu().findItem(R.id.action_filter_search);
    if (item == null) return;
    SearchView searchView = (SearchView) item.getActionView();
    if (searchView == null) return;
    EditText editText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
    if (editText == null) return;
    switch (currentField) {
      case AUTHOR:
        editText.setHint(R.string.deltax_search_by_author);
        break;
      case VERSION:
        editText.setHint(R.string.deltax_search_by_version);
        break;
      case PACKAGE:
        editText.setHint(R.string.deltax_search_by_package);
        break;
      default:
        editText.setHint(R.string.deltax_search_by_name);
        break;
    }
  }

  private void openFilePicker() {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/zip");
    try {
      pickerLauncher.launch(Intent.createChooser(intent, getString(R.string.deltax_install)));
    } catch (android.content.ActivityNotFoundException e) {
      Toast.makeText(this, R.string.deltax_install_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void installFromUri(Uri uri) {
    File tmp = new File(getCacheDir(), "deltax_install_" + System.currentTimeMillis() + ".zip");
    try (InputStream in = getContentResolver().openInputStream(uri);
        OutputStream out = new FileOutputStream(tmp)) {
      byte[] buf = new byte[8192];
      int len;
      while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
    } catch (Exception e) {
      if (tmp.exists()) tmp.delete();
      Toast.makeText(this, R.string.deltax_install_failed, Toast.LENGTH_SHORT).show();
      return;
    }
    if (deltaX.isBackupPackage(tmp)) {
      boolean ok = deltaX.restoreBackupFromZip(tmp);
      if (tmp.exists()) tmp.delete();
      refresh();
      if (ok) {
        Toast.makeText(this, R.string.deltax_restore_success, Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, R.string.deltax_restore_failed, Toast.LENGTH_SHORT).show();
      }
    } else {
      int n = deltaX.installPluginFromZip(tmp);
      if (tmp.exists()) tmp.delete();
      refresh();
      if (n > 0) {
        Toast.makeText(this, getString(R.string.deltax_install_success, n), Toast.LENGTH_SHORT)
            .show();
      } else {
        Toast.makeText(this, R.string.deltax_install_failed, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void refresh() {
    List<PluginInfo> plugins = getDisplayPlugins();
    boolean empty = plugins.isEmpty();
    recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    if (adapter == null) {
      adapter = new PluginAdapter(plugins, this);
      recycler.setAdapter(adapter);
    } else {
      adapter.setPlugins(plugins);
      adapter.setSelectionMode(selectionActive);
      adapter.setSelected(selectedIds);
      adapter.notifyDataSetChanged();
    }
  }

  @Override
  public void onToggle(PluginInfo plugin, boolean enabled) {
    deltaX.setPluginEnabled(plugin.getPackageName(), enabled);
    refresh();
  }

  @Override
  public void onUninstall(PluginInfo plugin) {
    new AlertDialog.Builder(this)
        .setTitle(R.string.deltax_uninstall)
        .setMessage(getString(R.string.deltax_confirm_uninstall, plugin.manifest.name))
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              deltaX.uninstallPlugin(plugin.getPackageName());
              refresh();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @Override
  public void onOpen(PluginInfo plugin) {
    Intent intent = new Intent(this, DeltaXPluginActivity.class);
    intent.putExtra(DeltaXPluginActivity.EXTRA_PACKAGE, plugin.getPackageName());
    startActivity(intent);
  }

  @Override
  public void onItemClick(PluginInfo plugin) {
    if (selectionActive) {
      toggleSelection(plugin);
    } else {
      onOpen(plugin);
    }
  }

  @Override
  public void onItemLongClick(PluginInfo plugin) {
    if (!selectionActive) {
      enterSelectionMode();
    }
    toggleSelection(plugin);
  }

  private void enterSelectionMode() {
    if (selectionActive) return;
    selectionActive = true;
    if (adapter != null) adapter.setSelectionMode(true);
    actionMode = startSupportActionMode(selectionCallback);
    updateSelectionTitle();
  }

  private void exitSelectionMode() {
    selectionActive = false;
    selectedIds.clear();
    if (actionMode != null) {
      actionMode.finish();
      actionMode = null;
    }
    if (adapter != null) {
      adapter.setSelectionMode(false);
      adapter.setSelected(selectedIds);
    }
  }

  private void toggleSelection(PluginInfo plugin) {
    String id = plugin.getPackageName();
    if (selectedIds.contains(id)) selectedIds.remove(id);
    else selectedIds.add(id);
    if (adapter != null) adapter.setSelected(selectedIds);
    updateSelectionTitle();
  }

  private void updateSelectionTitle() {
    if (actionMode != null) {
      actionMode.setTitle(getString(R.string.deltax_selected, selectedIds.size()));
    }
  }

  private List<PluginInfo> collectSelected() {
    List<PluginInfo> all = deltaX.getInstalledPlugins();
    List<PluginInfo> out = new ArrayList<>();
    for (PluginInfo p : all) {
      if (selectedIds.contains(p.getPackageName())) out.add(p);
    }
    return out;
  }

  private void exportSelectedPlugins() {
    List<PluginInfo> selected = collectSelected();
    if (selected.isEmpty()) {
      Toast.makeText(this, R.string.deltax_no_plugins_export, Toast.LENGTH_SHORT).show();
      exitSelectionMode();
      return;
    }
    File dir = DcHelper.getImexDir();
    dir.mkdirs();
    File target = new File(dir, buildExportFileName("selected-plugins"));
    boolean ok = deltaX.getPluginPackager().exportPlugins(target, selected);
    exitSelectionMode();
    if (ok) {
      Toast.makeText(
              this, getString(R.string.deltax_export_success, target.getName()), Toast.LENGTH_LONG)
          .show();
    } else {
      Toast.makeText(this, R.string.deltax_export_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void setSelectedEnabled(boolean enabled) {
    for (String id : new HashSet<>(selectedIds)) {
      deltaX.setPluginEnabled(id, enabled);
    }
    exitSelectionMode();
    refresh();
    Toast.makeText(
            this,
            enabled ? R.string.deltax_enabled_selected : R.string.deltax_disabled_selected,
            Toast.LENGTH_SHORT)
        .show();
  }

  private void deleteSelectedPlugins() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.deltax_delete_selected)
        .setMessage(getString(R.string.deltax_confirm_delete_selected, selectedIds.size()))
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              for (String id : new HashSet<>(selectedIds)) {
                deltaX.uninstallPlugin(id);
              }
              exitSelectionMode();
              refresh();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void enableAll(boolean enabled) {
    List<PluginInfo> all = deltaX.getInstalledPlugins();
    if (all.isEmpty()) return;
    for (PluginInfo p : all) {
      deltaX.setPluginEnabled(p.getPackageName(), enabled);
    }
    refresh();
    Toast.makeText(
            this,
            enabled ? R.string.deltax_enabled_all : R.string.deltax_disabled_all,
            Toast.LENGTH_SHORT)
        .show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.deltax_activity_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    if (item.getItemId() == R.id.action_export_all) {
      exportAllPlugins();
      return true;
    }
    if (item.getItemId() == R.id.action_export_backup) {
      exportBackupAll();
      return true;
    }
    if (item.getItemId() == R.id.action_enable_all) {
      enableAll(true);
      return true;
    }
    if (item.getItemId() == R.id.action_disable_all) {
      enableAll(false);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void exportAllPlugins() {
    List<PluginInfo> plugins = deltaX.getInstalledPlugins();
    if (plugins.isEmpty()) {
      Toast.makeText(this, R.string.deltax_no_plugins_export, Toast.LENGTH_SHORT).show();
      return;
    }

    File dir = DcHelper.getImexDir();
    dir.mkdirs();
    File target = new File(dir, buildExportFileName("plugins"));

    boolean ok = deltaX.getPluginPackager().exportAll(target);
    if (ok) {
      Toast.makeText(
              this, getString(R.string.deltax_export_success, target.getName()), Toast.LENGTH_LONG)
          .show();
    } else {
      Toast.makeText(this, R.string.deltax_export_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void exportBackupAll() {
    if (deltaX.getInstalledPlugins().isEmpty()) {
      Toast.makeText(this, R.string.deltax_no_plugins_export, Toast.LENGTH_SHORT).show();
      return;
    }

    File dir = DcHelper.getImexDir();
    dir.mkdirs();
    File target = new File(dir, buildExportFileName("backup"));

    boolean ok = deltaX.getPluginPackager().exportBackup(target);
    if (ok) {
      Toast.makeText(
              this, getString(R.string.deltax_backup_success, target.getName()), Toast.LENGTH_LONG)
          .show();
    } else {
      Toast.makeText(this, R.string.deltax_backup_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private String buildExportFileName(String kind) {
    String name = "user";
    String email = "unknown";
    try {
      DcContext dc = DcHelper.getContext(this);
      if (dc != null) {
        String displayName = dc.getConfig("displayname");
        if (displayName != null && !displayName.isEmpty()) name = displayName;
        String addr = dc.getConfig("addr");
        if (addr != null && !addr.isEmpty()) email = addr;
      }
    } catch (Exception ignored) {
    }

    String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
    return sanitize(name) + "-" + stamp + "-" + sanitize(email) + "-" + kind + ".zip";
  }

  private static String sanitize(String s) {
    if (s == null) return "";
    return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }

  private List<PluginInfo> getDisplayPlugins() {
    List<PluginInfo> all = deltaX.getInstalledPlugins();
    if (currentQuery == null || currentQuery.trim().isEmpty()) return all;
    String q = currentQuery.trim().toLowerCase(Locale.ROOT);
    List<PluginInfo> out = new ArrayList<>();
    for (PluginInfo p : all) {
      String value;
      switch (currentField) {
        case AUTHOR:
          value = p.manifest.author;
          break;
        case VERSION:
          value = p.manifest.version;
          break;
        case PACKAGE:
          value = p.getPackageName();
          break;
        default:
          value = p.manifest.name;
          break;
      }
      if (value != null && value.toLowerCase(Locale.ROOT).contains(q)) {
        out.add(p);
      }
    }
    return out;
  }

  private final ActionMode.Callback selectionCallback =
      new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
          mode.getMenuInflater().inflate(R.menu.deltax_selection_menu, menu);
          return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
          return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
          int id = item.getItemId();
          if (id == R.id.action_export_selected) {
            exportSelectedPlugins();
            return true;
          }
          if (id == R.id.action_enable_selected) {
            setSelectedEnabled(true);
            return true;
          }
          if (id == R.id.action_disable_selected) {
            setSelectedEnabled(false);
            return true;
          }
          if (id == R.id.action_delete_selected) {
            deleteSelectedPlugins();
            return true;
          }
          return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
          selectionActive = false;
          selectedIds.clear();
          if (adapter != null) {
            adapter.setSelectionMode(false);
            adapter.setSelected(selectedIds);
          }
          actionMode = null;
        }
      };
}
