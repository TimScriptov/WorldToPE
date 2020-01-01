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

import android.content.Context;
import net.zhuoweizhang.pocketinveditor.geo.ChunkManager;
import net.zhuoweizhang.pocketinveditor.io.xml.MaterialLoader;
import net.zhuoweizhang.pocketinveditor.material.Material;

public class BlockTranslationLoader {
    public static boolean blocksLoaded = false;

    public static void load(Context context) {
        if (!blocksLoaded) {
            new MaterialLoader(context.getResources().getXml(R.xml.item_data)).run();
            for (int i = 0; i < ChunkManager.WORLD_WIDTH; i++) {
                Converter.blockTranslate[i] = (byte) 35;
            }
            for (Material material : Material.materials) {
                int id = material.getId();
                if (id >= 0 && id < ChunkManager.WORLD_WIDTH) {
                    Converter.blockTranslate[id] = (byte) id;
                }
            }
            Converter.blockTranslate[125] = (byte) -99;
            Converter.blockTranslate[126] = (byte) -98;
            blocksLoaded = true;
        }
    }
}
