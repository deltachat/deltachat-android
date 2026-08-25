package org.thoughtcrime.securesms.deltax;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.List;
import org.thoughtcrime.securesms.R;

public class DeltaXActivity extends AppCompatActivity {

  private DeltaX deltaX;
  private TextView listView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_deltax);

    deltaX = DeltaX.getInstance(this);
    if (!deltaX.isInitialised()) {
      deltaX.init();
    }

    listView = findViewById(R.id.deltax_list);
    Button reloadButton = findViewById(R.id.deltax_reload);
    Button openFolderButton = findViewById(R.id.deltax_open_folder);

    reloadButton.setOnClickListener(
        v -> {
          deltaX.reloadPlugins();
          refresh();
          Toast.makeText(this, R.string.deltax_reloaded, Toast.LENGTH_SHORT).show();
        });

    openFolderButton.setOnClickListener(
        v -> {
          File pluginsDir = deltaX.getPluginsDir();
          Toast.makeText(this, pluginsDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
        });

    refresh();
  }

  private void refresh() {
    StringBuilder sb = new StringBuilder();
    List<String> lines = deltaX.getPluginList();
    for (String line : lines) {
      sb.append(line).append("\n");
    }
    sb.append("\n")
        .append(getString(R.string.deltax_plugins_dir))
        .append(": ")
        .append(deltaX.getPluginsDir().getAbsolutePath());
    listView.setText(sb.toString());
  }
}
