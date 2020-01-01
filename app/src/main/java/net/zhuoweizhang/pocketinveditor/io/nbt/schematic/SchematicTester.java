package net.zhuoweizhang.pocketinveditor.io.nbt.schematic;

import java.io.File;
import net.zhuoweizhang.pocketinveditor.geo.ChunkManager;
import net.zhuoweizhang.pocketinveditor.geo.CuboidClipboard;
import net.zhuoweizhang.pocketinveditor.util.Vector3f;

public final class SchematicTester {
    public static void main(String[] args) {
        try {
            ChunkManager mgr = new ChunkManager(new File(args[0]));
            CuboidClipboard schematic = SchematicIO.read(new File(args[1]));
            SchematicIO.save(schematic, new File(args[1] + ".new"));
            schematic.place(mgr, new Vector3f((float) Integer.parseInt(args[2]), (float) Integer.parseInt(args[3]), (float) Integer.parseInt(args[4])), false);
            System.out.println("Saving chunks...");
            System.out.println(mgr.saveAll() + " chunks saved");
            mgr.unloadChunks(false);
            mgr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
