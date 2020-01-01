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

import com.litl.leveldb.DB;
import com.litl.leveldb.WriteBatch;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.zhuoweizhang.pocketinveditor.Level;
import net.zhuoweizhang.pocketinveditor.entity.Player;
import net.zhuoweizhang.pocketinveditor.geo.Chunk;
import net.zhuoweizhang.pocketinveditor.geo.ChunkManager;
import net.zhuoweizhang.pocketinveditor.io.LevelDataConverter;
import net.zhuoweizhang.pocketinveditor.io.leveldb.DBKey;
import net.zhuoweizhang.pocketinveditor.io.leveldb.LevelDBConverter;
import net.zhuoweizhang.pocketinveditor.util.Vector3f;
import org.spout.nbt.ByteArrayTag;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.DoubleTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.ListTag;
import org.spout.nbt.StringTag;
import org.spout.nbt.Tag;
import org.spout.nbt.stream.NBTInputStream;

public class Converter {
    public static byte[] blockTranslate = new byte[ChunkManager.WORLD_WIDTH];
    private static Chunk peChunk = new Chunk(0, 0);
    private static DBKey tempKey = new DBKey();

    public interface ProgressListener {
        void onComplete();

        void onProgress(int i);
    }

    public static void convert(File inputDir, File outputDir, ProgressListener listener, boolean limit) throws IOException {
        File dbDir = new File(outputDir, "db");
        dbDir.mkdirs();
        Vector3f playerPos = setUpLevel(inputDir, outputDir).getPlayer().getLocation();
        DB db = LevelDBConverter.openDatabase(dbDir);
        if (db == null) {
            throw new RuntimeException("Database is null");
        }
        File[] regionFiles = new File(inputDir, "region").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(DesktopRegionFile.ANVIL_EXTENSION);
            }
        });
        int c = 0;
        for (File f : regionFiles) {
            listener.onProgress((int) ((((double) c) / ((double) regionFiles.length)) * 100.0d));
            convertRegion(f, db, playerPos, limit);
            c++;
        }
        db.close();
        System.gc();
        listener.onComplete();
    }

    private static void convertRegion(File regionFile, DB db, Vector3f center, boolean limit) throws IOException {
        System.out.println("Converting " + regionFile);
        int playerLimChunkX = ((int) center.getX()) >> 4;
        int playerLimChunkZ = ((int) center.getZ()) >> 4;
        int playerLimCount = limit ? 2 : -1;
        if (limit) {
            String[] regionParts = regionFile.getName().split("\\.");
            int regionMinX = Integer.parseInt(regionParts[1]) << 5;
            int regionMinZ = Integer.parseInt(regionParts[2]) << 5;
            if (playerLimChunkX >= regionMinX + 32 || playerLimChunkX + playerLimCount <= regionMinX || playerLimChunkZ >= regionMinZ + 32 || playerLimChunkZ + playerLimCount <= regionMinZ) {
                return;
            }
        }
        DesktopRegionFile region = new DesktopRegionFile(regionFile);
        WriteBatch batch = new WriteBatch();
        for (int x = 0; x < 32; x++) {
            boolean batchNeedsWrite = false;
            for (int z = 0; z < 32; z++) {
                if (region.hasChunk(x, z)) {
                    batchNeedsWrite = true;
                    DataInputStream is = region.getChunkDataInputStream(x, z);
                    if (is != null) {
                        convertChunk((CompoundTag) ((CompoundTag) new NBTInputStream(is, false).readTag()).getValue().get(0), batch, playerLimChunkX, playerLimChunkZ, playerLimCount);
                    }
                }
            }
            if (batchNeedsWrite) {
                db.write(batch);
                batch.clear();
            }
        }
    }

    private static void convertChunk(CompoundTag tag, WriteBatch batch, int limitX, int limitZ, int limitCount) {
        Map<String, Tag> tags = toMap(tag);
        int chunkX = ((IntTag) tags.get("xPos")).getValue().intValue();
        int chunkZ = ((IntTag) tags.get("zPos")).getValue().intValue();
        if (limitCount <= 0 || (chunkX >= limitX && chunkX < limitX + limitCount && chunkZ >= limitZ && chunkZ < limitZ + limitCount)) {
            System.out.println("Converting " + chunkX + ":" + chunkZ);
            zeroChunk(peChunk);
            for (CompoundTag sectionTag : ((ListTag) tags.get("Sections")).getValue()) {
                Map<String, Tag> section = toMap(sectionTag);
                int sectionY = ((ByteTag) section.get("Y")).getValue().byteValue();
                if (sectionY < 8) {
                    byte[] blocks = ((ByteArrayTag) section.get("Blocks")).getValue();
                    DataLayer data = new DataLayer(((ByteArrayTag) section.get("Data")).getValue(), 4);
                    DataLayer blockLight = new DataLayer(((ByteArrayTag) section.get("BlockLight")).getValue(), 4);
                    DataLayer dataLayer = new DataLayer(((ByteArrayTag) section.get("SkyLight")).getValue(), 4);
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 16; y++) {
                                int yy = (sectionY << 4) + y;
                                peChunk.setBlockTypeIdNoDirty(x, yy, z, blockTranslate[blocks[((z << 4) | x) | (y << 8)] & 255]);
                                peChunk.setBlockDataNoDirty(x, yy, z, data.get(x, y, z));
                                chunkSetHalf(peChunk, peChunk.blockLight, x, yy, z, blockLight.get(x, y, z));
                                chunkSetHalf(peChunk, peChunk.skyLight, x, yy, z, dataLayer.get(x, y, z));
                            }
                        }
                    }
                }
            }
            peChunk.setDirtyTable(0, 0, 0);
            batch.put(ByteBuffer.wrap(tempKey.setX(chunkX).setZ(chunkZ).setType(48).toBytes()), ByteBuffer.wrap(peChunk.saveToByteArray()));
        }
    }

    private static Level setUpLevel(File inputDir, File outputDir) throws IOException {
        NBTInputStream nbtIs = new NBTInputStream(new FileInputStream(new File(inputDir, "level.dat")));
        Map<String, Tag> pcLevel = toMap((CompoundTag) ((CompoundTag) nbtIs.readTag()).getValue().get(0));
        Map<String, Tag> pcPlayer = pcLevel.get("Player") != null ? toMap((CompoundTag) pcLevel.get("Player")) : null;
        nbtIs.close();
        Level level = new Level();
        level.setLevelName(((StringTag) pcLevel.get("LevelName")).getValue());
        level.setLastPlayed(System.currentTimeMillis() / 1000);
        level.setGameType(((IntTag) pcLevel.get("GameType")).getValue().intValue() == 1 ? 1 : 0);
        level.setSpawnX(((IntTag) pcLevel.get("SpawnX")).getValue().intValue());
        level.setSpawnY(((IntTag) pcLevel.get("SpawnY")).getValue().intValue());
        level.setSpawnZ(((IntTag) pcLevel.get("SpawnZ")).getValue().intValue());
        level.setGenerator(2);
        level.setEntities(new ArrayList<>());
        level.setTileEntities(new ArrayList<>());
        Player player = new Player();
        level.setPlayer(player);
        if (pcPlayer == null) {
            player.setLocation(new Vector3f((float) level.getSpawnX(), ((float) level.getSpawnY()) + 1.62f, (float) level.getSpawnZ()));
        } else {
            Vector3f vec = readDoubleVector((ListTag) pcPlayer.get("Pos"));
            vec.y += 1.62f;
            player.setLocation(vec);
        }
        player.setSpawnX(level.getSpawnX());
        player.setSpawnY(level.getSpawnY());
        player.setSpawnZ(level.getSpawnZ());
        player.setInventory(new ArrayList<>());
        player.setArmor(new ArrayList<>());
        player.getAbilities().initForGameType(level.getGameType());
        LevelDBConverter.writeLevel(level, new File(outputDir, "db"));
        LevelDataConverter.write(level, new File(outputDir, "level.dat"));
        return level;
    }

    private static Map<String, Tag> toMap(CompoundTag tag) {
        Map<String, Tag> tags = new HashMap<>();
        for (Tag t : tag.getValue()) {
            tags.put(t.getName(), t);
        }
        return tags;
    }

    private static void chunkSetHalf(Chunk chunk, byte[] arr, int x, int y, int z, int newData) {
        int offset = (((x * ChunkManager.WORLD_HEIGHT) * 16) + (z * ChunkManager.WORLD_HEIGHT)) + y;
        byte oldData = arr[offset >> 1];
        if ((offset & 1) == 1) {
            arr[offset >> 1] = (byte) ((newData << 4) | (oldData & 15));
        } else {
            arr[offset >> 1] = (byte) ((oldData & 240) | (newData & 15));
        }
    }

    private static Vector3f readDoubleVector(ListTag<DoubleTag> tag) {
        List<DoubleTag> tags = tag.getValue();
        return new Vector3f(tags.get(0).getValue().floatValue(), tags.get(1).getValue().floatValue(), tags.get(2).getValue().floatValue());
    }

    private static void zeroChunk(Chunk chunk) {
        Arrays.fill(chunk.blocks, (byte) 0);
        Arrays.fill(chunk.blockLight, (byte) 0);
        Arrays.fill(chunk.skyLight, (byte) 0);
        Arrays.fill(chunk.metaData, (byte) 0);
        Arrays.fill(chunk.grassColor, (byte) 0);
    }
}
