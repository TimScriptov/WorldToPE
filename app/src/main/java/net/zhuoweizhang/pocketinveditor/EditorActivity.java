package net.zhuoweizhang.pocketinveditor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.ClipboardManager;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.mcal.worldtope.R;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import net.zhuoweizhang.pocketinveditor.io.LevelDataConverter;
import net.zhuoweizhang.pocketinveditor.io.xml.MaterialIconLoader;
import net.zhuoweizhang.pocketinveditor.io.xml.MaterialLoader;
import net.zhuoweizhang.pocketinveditor.io.zip.ZipFileWriter;

public class EditorActivity extends Activity {
    public static Level level;
    private static Object loadLock = new Object();
    public static File worldFolder;
    protected Button copySeedButton;
    protected Button editTerrainButton;
    protected Button entitiesInfoButton;
    protected Button promoButton;
    private Button startBackupButton;
    private Button startInventoryEditorButton;
    private Button startWorldInfoButton;
    protected Button viewTileEntitiesButton;
    private TextView worldLastPlayedView;
    private Thread worldLoadingThread;
    private TextView worldNameView;
    private TextView worldSeedView;

    private class BackupTask implements Runnable {
        private File backupFile;
        private File worldFolder;

        public BackupTask(File worldFolder, File backupFile) {
            this.worldFolder = worldFolder;
            this.backupFile = backupFile;
        }

        public void run() {
            try {
                ZipFileWriter.write(worldFolder.listFiles(), backupFile);
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(EditorActivity.this, getResources().getText(R.string.backupcreated) + backupFile.getAbsolutePath(), 1).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(EditorActivity.this, R.string.backupfailed, 1).show();
                    }
                });
            }
        }
    }

    private class LevelLoadTask implements Runnable {
        public void run() {
            try {
                synchronized (EditorActivity.loadLock) {
                    System.out.println("Loading level data from EditorActivity");
                    final Level level = LevelDataConverter.read(new File(EditorActivity.worldFolder, "level.dat"));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            levelLoaded(level);
                        }
                    });
                }
            } catch (final Exception e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        System.err.println("Failed to load");
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*setContentView(R.layout.main_pocketinveditor);
        this.worldNameView = (TextView) findViewById(R.id.main_world_name);
        this.worldLastPlayedView = (TextView) findViewById(R.id.main_lastplayed);
        this.worldSeedView = (TextView) findViewById(R.id.main_seed);
        this.startInventoryEditorButton = (Button) findViewById(R.id.main_edit_inventory);
        this.startInventoryEditorButton.setEnabled(false);
        this.startInventoryEditorButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditorActivity.this.startInventoryEditor();
            }
        });
        this.startBackupButton = (Button) findViewById(R.id.main_backup);
        this.startBackupButton.setEnabled(false);
        this.startBackupButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditorActivity.this.startBackupWorld();
            }
        });
        this.startWorldInfoButton = (Button) findViewById(R.id.main_world_info);
        this.startWorldInfoButton.setEnabled(false);
        this.startWorldInfoButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditorActivity.this.startWorldInfo();
            }
        });
        this.entitiesInfoButton = (Button) findViewById(R.id.main_entities_info);
        this.entitiesInfoButton.setEnabled(false);
        this.entitiesInfoButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditorActivity.this.startEntitiesInfo();
            }
        });
        this.viewTileEntitiesButton = (Button) findViewById(R.id.main_view_tileentities);
        this.viewTileEntitiesButton.setEnabled(false);
        this.viewTileEntitiesButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditorActivity.this.startViewTileEntities();
            }
        });
        this.editTerrainButton = (Button) findViewById(R.id.main_edit_terrain);
        this.editTerrainButton.setEnabled(false);
        this.editTerrainButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditorActivity.this.startEditTerrain();
            }
        });
        this.copySeedButton = (Button) findViewById(R.id.main_copy_seed);
        this.copySeedButton.setEnabled(false);
        this.copySeedButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditorActivity.this.copySeedToClipboard();
            }
        });
        worldFolder = new File(getIntent().getStringExtra("world"));
        loadLevel();
        if (Material.materials == null) {
            loadMaterials();
        }*/
    }

    private void loadMaterials() {
        new Thread(new MaterialLoader(getResources().getXml(R.xml.item_data))).start();
        new Thread(new MaterialIconLoader(this)).start();
    }

    protected void onRestart() {
        super.onRestart();
        updateUiAfterLevelLoad();
    }

    private void loadLevel() {
        worldLoadingThread = new Thread(new LevelLoadTask());
        worldLoadingThread.start();
    }

    private void levelLoaded(Level level) {
        this.level = level;
        updateUiAfterLevelLoad();
    }

    private void updateUiAfterLevelLoad() {
        worldNameView.setText(level.getLevelName());
        worldLastPlayedView.setText(getResources().getText(R.string.lastplayed) + ": " + DateFormat.getInstance().format(new Date(level.getLastPlayed() * 1000)));
        worldSeedView.setText(Long.toString(level.getRandomSeed()));
        startInventoryEditorButton.setEnabled(true);
        startBackupButton.setEnabled(true);
        startWorldInfoButton.setEnabled(true);
        entitiesInfoButton.setEnabled(true);
        viewTileEntitiesButton.setEnabled(true);
        editTerrainButton.setEnabled(true);
        copySeedButton.setEnabled(true);
    }

    protected void startInventoryEditor() {
        startActivityWithExtras(new Intent(this, InventorySlotsActivity.class));
    }

    public static void save(final Activity context) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized (EditorActivity.loadLock) {
                        System.out.println("Saving level.dat for Activity " + context);
                        LevelDataConverter.write(EditorActivity.level, new File(EditorActivity.worldFolder, "level.dat"));
                    }
                    System.out.println("... LIKE A BOSS!");
                    if (context != null) {
                        context.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(context, R.string.saved, 0).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (context != null) {
                        context.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(context, R.string.savefailed, 0).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void startBackupWorld() {
        File backupFolder = new File(new File(Environment.getExternalStorageDirectory(), "games/com.mojang/minecraftWorlds_backup"), worldFolder.getName());
        backupFolder.mkdirs();
        String currentTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US).format(new Date());
        File backupFile = new File(backupFolder, worldFolder.getName() + currentTime + ".zip");
        int postFix = 1;
        while (backupFile.exists()) {
            postFix++;
            backupFile = new File(backupFolder, worldFolder.getName() + currentTime + "_" + postFix + ".zip");
        }
        Toast.makeText(this, R.string.backup_start, 1).show();
        new Thread(new BackupTask(worldFolder, backupFile)).start();
    }

    public void copySeedToClipboard() {
        try {
            ((ClipboardManager) getSystemService("clipboard")).setText(Long.toString(level.getRandomSeed()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startWorldInfo() {
        startActivityWithExtras(new Intent(this, WorldInfoActivity.class));
    }

    public void startEntitiesInfo() {
        startActivityWithExtras(new Intent(this, EntitiesInfoActivity.class));
    }

    public void startViewTileEntities() {
        Intent intent = new Intent(this, TileEntityViewActivity.class);
        intent.putExtra("CanEditSlots", false);
        startActivityWithExtras(intent);
    }

    public void startEditTerrain() {
        throw new UnsupportedOperationException("Not in free version!");
    }

    public void startActivityWithExtras(Intent intent) {
        intent.putExtras(getIntent());
        startActivity(intent);
    }

    public void launchPromo() {
        try {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=net.zhuoweizhang.mcpelauncher"));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        finish();
        return true;
    }

    public static void loadLevelData(final Activity activity, final LevelDataLoadListener listener, final String location) {
        worldFolder = new File(location);
        new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized (EditorActivity.loadLock) {
                        System.out.println("Loading level data:" + activity + ":" + listener + ":" + location);
                        final Level level = LevelDataConverter.read(new File(EditorActivity.worldFolder, "level.dat"));
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                EditorActivity.level = level;
                                listener.onLevelDataLoad();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
