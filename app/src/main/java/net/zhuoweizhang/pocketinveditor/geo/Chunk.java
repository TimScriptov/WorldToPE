package net.zhuoweizhang.pocketinveditor.geo;

public class Chunk {
	/** height on the Y axis */
    public static final int HEIGHT = 128;
	/** length on the Z axis */
    public static final int LENGTH = 16;
	/** width on the X axis */
    public static final int WIDTH = 16;
    public byte[] blockLight;
    public byte[] blocks;
    public byte[] dirtyTable;
    public byte[] grassColor;
    private boolean hasFilledDirtyTable = false;
    public byte[] metaData;
    public boolean needsSaving = false;
    public byte[] skyLight;
    public final int x;
    public final int z;

    public static final class Key {
        private int x;
        private int z;

        public Key(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public Key(Key other) {
            this(other.x, other.z);
        }

        public int getX() {
            return this.x;
        }

        public int getZ() {
            return this.z;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setZ(int z) {
            this.z = z;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key)) {
                return false;
            }
            Key ok = (Key) other;
            if (ok.getX() == this.x && ok.getZ() == this.z) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return ((this.x + 31) * 31) + this.z;
        }
    }

    public Chunk(int x, int z) {
        this.x = x;
        this.z = z;
        this.blocks = new byte[32768];
        this.metaData = new byte[16384];
        this.blockLight = new byte[16384];
        this.skyLight = new byte[16384];
        this.dirtyTable = new byte[ChunkManager.WORLD_WIDTH];
        this.grassColor = new byte[768];
    }

    public void loadFromByteArray(byte[] rawData) {
        System.arraycopy(rawData, 0, this.blocks, 0, this.blocks.length);
        int offset = 0 + this.blocks.length;
        System.arraycopy(rawData, offset, this.metaData, 0, this.metaData.length);
        offset += this.metaData.length;
        System.arraycopy(rawData, offset, this.skyLight, 0, this.skyLight.length);
        offset += this.skyLight.length;
        System.arraycopy(rawData, offset, this.blockLight, 0, this.blockLight.length);
        offset += this.blockLight.length;
        System.arraycopy(rawData, offset, this.dirtyTable, 0, this.dirtyTable.length);
        System.arraycopy(rawData, offset + this.dirtyTable.length, this.grassColor, 0, this.grassColor.length);
    }

    public byte[] saveToByteArray() {
        byte[] retval = new byte[((((((this.blocks.length + this.metaData.length) + this.skyLight.length) + this.blockLight.length) + this.dirtyTable.length) + this.grassColor.length) + 3)];
        System.arraycopy(this.blocks, 0, retval, 0, this.blocks.length);
        int offset = 0 + this.blocks.length;
        System.arraycopy(this.metaData, 0, retval, offset, this.metaData.length);
        offset += this.metaData.length;
        System.arraycopy(this.skyLight, 0, retval, offset, this.skyLight.length);
        offset += this.skyLight.length;
        System.arraycopy(this.blockLight, 0, retval, offset, this.blockLight.length);
        offset += this.blockLight.length;
        System.arraycopy(this.dirtyTable, 0, retval, offset, this.dirtyTable.length);
        System.arraycopy(this.grassColor, 0, retval, offset + this.dirtyTable.length, this.grassColor.length);
        return retval;
    }
	
	/** Calculates and returns the number of diamond ore blocks in this chunk */
	
    public int countDiamonds() {
        int count = 0;
        for (byte b : this.blocks) {
            if (b == (byte) 56) {
                count++;
            }
        }
        return count;
    }

    public boolean dirtyTableIsReallyGross() {
        for (byte b : this.dirtyTable) {
            if (b != (byte) 0) {
                return true;
            }
        }
        return false;
    }

    public int getBlockTypeId(int x, int y, int z) {
        if (x >= WIDTH || y >= HEIGHT || z >= WIDTH || x < 0 || y < 0 || z < 0) {
            return 0;
        }
        int typeId = this.blocks[getOffset(x, y, z)];
        if (typeId < 0) {
            return typeId + ChunkManager.WORLD_WIDTH;
        }
        return typeId;
    }

    public int getBlockData(int x, int y, int z) {
        if (x >= WIDTH || y >= HEIGHT || z >= WIDTH || x < 0 || y < 0 || z < 0) {
            return 0;
        }
        int offset = getOffset(x, y, z);
        int dualData = this.metaData[offset >> 1];
        return offset % 2 == 1 ? (dualData >> 4) & 15 : dualData & 15;
    }
	
	/** Sets a block type, and also set the corresponding dirty table entry and set the saving flag. */
    public void setBlockTypeId(int x, int y, int z, int type) {
        if (x < WIDTH && y < HEIGHT && z < WIDTH && x >= 0 && y >= 0 && z >= 0) {
            setBlockTypeIdNoDirty(x, y, z, type);
            setDirtyTable(x, y, z);
            setNeedsSaving(true);
        }
    }

    public void setBlockTypeIdNoDirty(int x, int y, int z, int type) {
		//System.out.println(this.x + ":" + this.z + " setBlockTypeIdNoDirty: " + x + ":" + y + ":" + z);
        this.blocks[getOffset(x, y, z)] = (byte) type;
    }

    public void setBlockData(int x, int y, int z, int newData) {
        if (x < WIDTH && y < HEIGHT && z < WIDTH && x >= 0 && y >= 0 && z >= 0) {
            setBlockDataNoDirty(x, y, z, newData);
            setDirtyTable(x, y, z);
            setNeedsSaving(true);
        }
    }

    public void setBlockDataNoDirty(int x, int y, int z, int newData) {
        int offset = getOffset(x, y, z);
		byte oldData = metaData[offset >> 1];
		if (offset % 2 == 1) {
			metaData[offset >> 1] = (byte) ((newData << 4) | (oldData & 0xf));
		} else {
			metaData[offset >> 1] = (byte) ((oldData & 0xf0) | (newData & 0xf));
		}
    }

    public void setDirtyTable(int x, int y, int z) {
        if (hasFilledDirtyTable) return;
		//System.err.println("BRUTE FORCE AND IGNORANCE BEGIN! setDirtytable : fixme");
		for (int i = 0; i < 256; i++) {
			dirtyTable[i] = (byte) 0xff;
		}
		hasFilledDirtyTable = true;
    }
	
	/** Does this chunk need saving next time the ChunkManager issues a save? */
    public void setNeedsSaving(boolean save) {
        this.needsSaving = save;
    }

    public int getHighestBlockYAt(int x, int z) {
        for (int y = 127; y >= 0; --y) {
			if (getBlockTypeId(x, y, z) != 0) {
				return y;
			}
		}
		return 0;
    }

    private static int getOffset(int x, int y, int z) {
        return (x * HEIGHT * LENGTH) + (z * HEIGHT) + y;
    }
}
