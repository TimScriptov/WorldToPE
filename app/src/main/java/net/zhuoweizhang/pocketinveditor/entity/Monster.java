package net.zhuoweizhang.pocketinveditor.entity;

public class Monster extends LivingEntity {
    private boolean spawnedAtNight = true;

    public boolean isSpawnedAtNight() {
        return spawnedAtNight;
    }

    public void setSpawnedAtNight(boolean spawnedAtNight) {
        this.spawnedAtNight = spawnedAtNight;
    }
}
