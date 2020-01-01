package net.zhuoweizhang.pocketinveditor.io.nbt.entity;

import java.util.List;
import net.zhuoweizhang.pocketinveditor.entity.Minecart;
import org.spout.nbt.Tag;

public class MinecartEntityStore<T extends Minecart> extends EntityStore<T> {
    public void loadTag(T entity, Tag tag) {
        super.loadTag(entity, tag);
    }

    public List<Tag> save(T entity) {
        return super.save(entity);
    }
}
