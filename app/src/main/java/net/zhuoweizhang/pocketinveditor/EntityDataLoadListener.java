package net.zhuoweizhang.pocketinveditor;

import net.zhuoweizhang.pocketinveditor.io.EntityDataConverter.EntityData;

public interface EntityDataLoadListener {
    void onEntitiesLoaded(EntityData entityData);
}
