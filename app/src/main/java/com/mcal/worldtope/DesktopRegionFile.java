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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class DesktopRegionFile {
    public static final String ANVIL_EXTENSION = ".mca";
    static final int CHUNK_HEADER_SIZE = 5;
    public static final String MCREGION_EXTENSION = ".mcr";
    private static final int SECTOR_BYTES = 4096;
    private static final int SECTOR_INTS = 1024;
    private static final int VERSION_DEFLATE = 2;
    private static final int VERSION_GZIP = 1;
    private static final byte[] emptySector = new byte[SECTOR_BYTES];
    private final int[] chunkTimestamps = new int[SECTOR_INTS];
    private RandomAccessFile file;
    private final File fileName;
    private long lastModified = 0;
    private final int[] offsets = new int[SECTOR_INTS];
    private ArrayList<Boolean> sectorFree;
    private int sizeDelta;

    class ChunkBuffer extends ByteArrayOutputStream {
        private int x;
        private int z;

        public ChunkBuffer(int x, int z) {
            super(8096);
            this.x = x;
            this.z = z;
        }

        public void close() {
            DesktopRegionFile.this.write(this.x, this.z, this.buf, this.count);
        }
    }

    public DesktopRegionFile(File path) {
        this.fileName = path;
        debugln("REGION LOAD " + fileName);
        this.sizeDelta = 0;
        try {
            int i;
            if (path.exists()) {
                lastModified = path.lastModified();
            }
            this.file = new RandomAccessFile(path, "rw");
            if (file.length() < 4096) {
                for (i = 0; i < SECTOR_INTS; i += VERSION_GZIP) {
                    file.writeInt(0);
                }
                for (i = 0; i < SECTOR_INTS; i += VERSION_GZIP) {
                    file.writeInt(0);
                }
                sizeDelta += 8192;
            }
            if ((file.length() & 4095) != 0) {
                for (i = 0; ((long) i) < (file.length() & 4095); i += VERSION_GZIP) {
                    file.write(0);
                }
            }
            int nSectors = ((int) file.length()) / SECTOR_BYTES;
            sectorFree = new ArrayList<>(nSectors);
            for (i = 0; i < nSectors; i += VERSION_GZIP) {
                sectorFree.add(Boolean.valueOf(true));
            }
            sectorFree.set(0, Boolean.valueOf(false));
            sectorFree.set(VERSION_GZIP, Boolean.valueOf(false));
            file.seek(0);
            for (i = 0; i < SECTOR_INTS; i += VERSION_GZIP) {
                int offset = file.readInt();
                offsets[i] = offset;
                if (offset != 0 && (offset >> 8) + (offset & 255) <= sectorFree.size()) {
                    for (int sectorNum = 0; sectorNum < (offset & 255); sectorNum += VERSION_GZIP) {
                        sectorFree.set((offset >> 8) + sectorNum, Boolean.valueOf(false));
                    }
                }
            }
            for (i = 0; i < SECTOR_INTS; i += VERSION_GZIP) {
                chunkTimestamps[i] = file.readInt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long lastModified() {
        return lastModified;
    }

    public synchronized int getSizeDelta() {
        int ret;
        ret = this.sizeDelta;
        sizeDelta = 0;
        return ret;
    }

    private void debug(String in) {
    }

    private void debugln(String in) {
        debug(in + "\n");
    }

    private void debug(String mode, int x, int z, String in) {
        debug("REGION " + mode + " " + fileName.getName() + "[" + x + "," + z + "] = " + in);
    }

    private void debug(String mode, int x, int z, int count, String in) {
        debug("REGION " + mode + " " + fileName.getName() + "[" + x + "," + z + "] " + count + "B = " + in);
    }

    private void debugln(String mode, int x, int z, String in) {
        debug(mode, x, z, in + "\n");
    }

    public synchronized DataInputStream getChunkDataInputStream(int x, int z) {
        DataInputStream dataInputStream;
        if (outOfBounds(x, z)) {
            debugln("READ", x, z, "out of bounds");
            dataInputStream = null;
        } else {
            try {
                int offset = getOffset(x, z);
                if (offset == 0) {
                    dataInputStream = null;
                } else {
                    int sectorNumber = offset >> 8;
                    int numSectors = offset & 255;
                    if (sectorNumber + numSectors > sectorFree.size()) {
                        debugln("READ", x, z, "invalid sector");
                        dataInputStream = null;
                    } else {
                        this.file.seek((long) (sectorNumber * SECTOR_BYTES));
                        int length = file.readInt();
                        if (length > numSectors * SECTOR_BYTES) {
                            debugln("READ", x, z, "invalid length: " + length + " > 4096 * " + numSectors);
                            dataInputStream = null;
                        } else {
                            byte version = this.file.readByte();
                            byte[] data;
                            if (version == (byte) 1) {
                                data = new byte[(length - 1)];
                                file.read(data);
                                dataInputStream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(data))));
                            } else if (version == (byte) 2) {
                                data = new byte[(length - 1)];
                                file.read(data);
                                dataInputStream = new DataInputStream(new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(data))));
                            } else {
                                debugln("READ", x, z, "unknown version " + version);
                                dataInputStream = null;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                debugln("READ", x, z, "exception");
                dataInputStream = null;
            }
        }
        return dataInputStream;
    }

    public DataOutputStream getChunkDataOutputStream(int x, int z) {
        if (outOfBounds(x, z)) {
            return null;
        }
        return new DataOutputStream(new DeflaterOutputStream(new ChunkBuffer(x, z)));
    }

    protected synchronized void write(int x, int z, byte[] data, int length) {
        try {
            int offset = getOffset(x, z);
            int sectorNumber = offset >> 8;
            int sectorsAllocated = offset & 255;
            int sectorsNeeded = ((length + CHUNK_HEADER_SIZE) / SECTOR_BYTES) + VERSION_GZIP;
            if (sectorsNeeded < 256) {
                if (sectorNumber == 0 || sectorsAllocated != sectorsNeeded) {
                    int i;
                    for (i = 0; i < sectorsAllocated; i += VERSION_GZIP) {
                        sectorFree.set(sectorNumber + i, Boolean.valueOf(true));
                    }
                    int runStart = sectorFree.indexOf(Boolean.valueOf(true));
                    int runLength = 0;
                    if (runStart != -1) {
                        for (i = runStart; i < sectorFree.size(); i += VERSION_GZIP) {
                            if (runLength != 0) {
                                if (sectorFree.get(i).booleanValue()) {
                                    runLength += VERSION_GZIP;
                                } else {
                                    runLength = 0;
                                }
                            } else if (sectorFree.get(i).booleanValue()) {
                                runStart = i;
                                runLength = VERSION_GZIP;
                            }
                            if (runLength >= sectorsNeeded) {
                                break;
                            }
                        }
                    }
                    if (runLength >= sectorsNeeded) {
                        debug("SAVE", x, z, length, "reuse");
                        sectorNumber = runStart;
                        setOffset(x, z, (sectorNumber << 8) | sectorsNeeded);
                        for (i = 0; i < sectorsNeeded; i += VERSION_GZIP) {
                            sectorFree.set(sectorNumber + i, Boolean.valueOf(false));
                        }
                        write(sectorNumber, data, length);
                    } else {
                        debug("SAVE", x, z, length, "grow");
                        file.seek(file.length());
                        sectorNumber = sectorFree.size();
                        for (i = 0; i < sectorsNeeded; i += VERSION_GZIP) {
                            file.write(emptySector);
                            sectorFree.add(Boolean.valueOf(false));
                        }
                        sizeDelta += sectorsNeeded * SECTOR_BYTES;
                        write(sectorNumber, data, length);
                        setOffset(x, z, (sectorNumber << 8) | sectorsNeeded);
                    }
                } else {
                    debug("SAVE", x, z, length, "rewrite");
                    write(sectorNumber, data, length);
                }
                setTimestamp(x, z, (int) (System.currentTimeMillis() / 1000));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(int sectorNumber, byte[] data, int length) throws IOException {
        debugln(" " + sectorNumber);
        file.seek((long) (sectorNumber * SECTOR_BYTES));
        file.writeInt(length + VERSION_GZIP);
        file.writeByte(VERSION_DEFLATE);
        file.write(data, 0, length);
    }

    private boolean outOfBounds(int x, int z) {
        return x < 0 || x >= 32 || z < 0 || z >= 32;
    }

    private int getOffset(int x, int z) {
        return offsets[(z * 32) + x];
    }

    public boolean hasChunk(int x, int z) {
        return getOffset(x, z) != 0;
    }

    private void setOffset(int x, int z, int offset) throws IOException {
        offsets[(z * 32) + x] = offset;
        file.seek((long) (((z * 32) + x) * 4));
        file.writeInt(offset);
    }

    private void setTimestamp(int x, int z, int value) throws IOException {
        chunkTimestamps[(z * 32) + x] = value;
        file.seek((long) ((((z * 32) + x) * 4) + SECTOR_BYTES));
        file.writeInt(value);
    }

    public void close() throws IOException {
        file.close();
    }
}
