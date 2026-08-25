package org.thoughtcrime.securesms.deltax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.b44t.messenger.DcContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.deltax.module.PluginInfo;

public class DeltaXActivity extends BaseActionBarActivity
    implements PluginAdapter.PluginActionListener {

  private DeltaX deltaX;
  private RecyclerView recycler;
  private TextView emptyView;
  private ActivityResultLauncher<Intent> pickerLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_deltax);

    ActionBar ab = getSupportActionBar();
    if (ab != null) {
      ab.setDisplayHomeAsUpEnabled(true);
    }

    deltaX = DeltaX.getInstance(this);
    if (!deltaX.isInitialised()) {
      deltaX.init();
    }

    recycler = findViewById(R.id.deltax_recycler);
    recycler.setLayoutManager(new LinearLayoutManager(this));
    emptyView = findViewById(R.id.deltax_empty);

    findViewById(R.id.deltax_fab).setOnClickListener(v -> openFilePicker());

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

  private void refresh() {
    List<PluginInfo> plugins = deltaX.getInstalledPlugins();
    boolean empty = plugins.isEmpty();
    recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    recycler.setAdapter(new PluginAdapter(plugins, this));
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
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.deltax_activity_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(android.view.MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    if (item.getItemId() == R.id.action_export_all) {
      exportAllPlugins();
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
    String fileName = sanitize(name) + "-" + stamp + "-" + sanitize(email) + ".zip";

    File dir = DcHelper.getImexDir();
    dir.mkdirs();
    File target = new File(dir, fileName);

    boolean ok = deltaX.getPluginPackager().exportAll(target);
    if (ok) {
      Toast.makeText(
              this, getString(R.string.deltax_export_success, target.getName()), Toast.LENGTH_LONG)
          .show();
    } else {
      Toast.makeText(this, R.string.deltax_export_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private static String sanitize(String s) {
    if (s == null) return "";
    return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }
}
