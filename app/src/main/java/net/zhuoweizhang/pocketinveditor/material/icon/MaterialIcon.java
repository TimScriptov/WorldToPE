package net.zhuoweizhang.pocketinveditor.material.icon;

import android.graphics.Bitmap;
import java.util.Map;
import net.zhuoweizhang.pocketinveditor.material.MaterialKey;

public class MaterialIcon {
    public static Map<MaterialKey, MaterialIcon> icons;
    public Bitmap bitmap;
    public short damage;
    public int typeId;

    public MaterialIcon(int typeId, short damage, Bitmap bmp) {
        this.typeId = typeId;
        this.damage = damage;
        this.bitmap = bmp;
    }
}
