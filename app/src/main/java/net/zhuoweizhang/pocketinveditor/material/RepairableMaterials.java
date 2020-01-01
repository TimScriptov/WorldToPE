package net.zhuoweizhang.pocketinveditor.material;

import java.util.HashSet;
import java.util.Set;
import net.zhuoweizhang.pocketinveditor.ItemStack;
import net.zhuoweizhang.pocketinveditor.geo.ChunkManager;

/** A list of all materials in Minecraft PE that uses their Damage values to calculate... damage. */
public final class RepairableMaterials {
    public static final Set<Integer> ids = new HashSet<>();

    static {
        add(ChunkManager.WORLD_WIDTH, 259);
        add(261);
        add(267, 279);
        add(283, 286);
        add(298, 317);
        add(359);
    }

    private static void add(int id) {
        ids.add(Integer.valueOf(id));
    }

    private static void add(int begin, int end) {
        for (int i = begin; i <= end; i++) {
            ids.add(Integer.valueOf(i));
        }
    }

	/** returns whether this stack can be repaired by resetting its damage value; */
    public static boolean isRepairable(ItemStack stack) {
        return ids.contains(new Integer(stack.getTypeId()));
    }
}
