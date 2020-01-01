/*
 * Copyright (C) 2019-2020 Тимашков Иван
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.mcal.worldtope;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mcal.worldtope.Converter.ProgressListener;
import java.io.File;

public class MainActivity extends Activity implements ProgressListener {
    private TextView convertingText;
    private EditText inputDirNameText;
    private EditText outputDirNameText;
    private ProgressBar progressBar;
    private View selectWorldLayout;
    private File worldToUse;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        progressBar = findViewById(R.id.progress_bar);
        selectWorldLayout = findViewById(R.id.select_world_layout);
        inputDirNameText = findViewById(R.id.input_dir_text);
        outputDirNameText = findViewById(R.id.output_dir_text);
        convertingText = findViewById(R.id.converting_text);
        BlockTranslationLoader.load(this);
        setIsRunning(false);
		if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Settings.ACTION_MANAGE_OVERLAY_PERMISSION}, 1);
            }
        }
    }

    public void startConvertion(View v) {
        if (this.worldToUse != null) {
            setIsRunning(true);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Converter.convert(worldToUse, new File("/sdcard/games/com.mojang/minecraftWorlds/", outputDirNameText.getText().toString()), MainActivity.this, isLimited());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    public void onProgress(final int percent) {
        this.progressBar.post(new Runnable() {
            public void run() {
                progressBar.setProgress(percent);
            }
        });
    }

    public void onComplete() {
        this.progressBar.post(new Runnable() {
            public void run() {
                setIsRunning(false);
                Toast.makeText(MainActivity.this, R.string.convertion_complete, 0).show();
            }
        });
    }

    public void setIsRunning(boolean yep) {
        int i;
        int i2 = 0;
        View view = selectWorldLayout;
        if (yep) {
            i = 8;
        } else {
            i = 0;
        }
        view.setVisibility(i);
        ProgressBar progressBar = this.progressBar;
        if (yep) {
            i = 0;
        } else {
            i = 8;
        }
        progressBar.setVisibility(i);
        TextView textView = convertingText;
        if (!yep) {
            i2 = 8;
        }
        textView.setVisibility(i2);
    }

    public void selectWorld(View v) {
        Intent target = FileUtils.createGetContentIntent();
        target.setType("application/x-level-dat");
        target.setClass(this, FileChooserActivity.class);
        startActivityForResult(target, 1224);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1224:
                if (resultCode == -1) {
                    worldToUse = FileUtils.getFile(data.getData()).getParentFile();
                    outputDirNameText.setText(worldToUse.getName());
                    inputDirNameText.setText(worldToUse.getAbsolutePath());
                    return;
                }
                return;
            default:
                return;
        }
    }

    protected boolean isLimited() {
        return false;
    }
}
