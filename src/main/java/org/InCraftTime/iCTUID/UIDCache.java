package org.InCraftTime.iCTUID;

// 使用简单缓存
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UIDCache {
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    public void put(UUID uuid, String uid) {
        cache.put(uuid, uid);
    }

    public String get(UUID uuid) {
        return cache.get(uuid);
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
    }
}