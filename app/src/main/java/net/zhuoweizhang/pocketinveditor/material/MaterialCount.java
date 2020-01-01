package net.zhuoweizhang.pocketinveditor.material;

public class MaterialCount {
    public int count;
    public MaterialKey key;

    public MaterialCount(MaterialKey key, int count) {
        this.key = key;
        this.count = count;
    }

    public String toString() {
        return "[" + this.key.typeId + ":" + this.key.damage + "]: " + this.count;
    }
}
