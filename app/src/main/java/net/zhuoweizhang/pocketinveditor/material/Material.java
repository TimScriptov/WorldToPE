package net.zhuoweizhang.pocketinveditor.material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Material {
    public static Map<MaterialKey, Material> materialMap = new HashMap<>();
    public static List<Material> materials;
    private short damage;
    private boolean damageable;
    private boolean hasSubtypes;
    private int id;
    private String name;

    public Material(int id, String name) {
        this(id, name, (short) 0, false);
    }

    public Material(int id, String name, short damage) {
        this(id, name, damage, true);
    }

    public Material(int id, String name, short damage, boolean hasSubtypes) {
        this.damageable = false;
        this.id = id;
        this.name = name;
        this.damage = damage;
        this.hasSubtypes = hasSubtypes;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public short getDamage() {
        return this.damage;
    }

    public boolean hasSubtypes() {
        return this.hasSubtypes;
    }

    public void setDamageable(boolean damageable) {
        this.damageable = damageable;
    }

    public boolean isDamageable() {
        return this.damageable;
    }

    public String toString() {
        return getName() + " : " + getId() + (this.damage != (short) 0 ? ":" + this.damage : "");
    }
}
