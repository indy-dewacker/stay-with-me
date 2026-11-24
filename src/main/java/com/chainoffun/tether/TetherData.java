package com.chainoffun.tether;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All tethers on the server, saved with the world. */
public class TetherData extends SavedData {
    private static final String NAME = "chainoffun_links";

    public static final class Link {
        public final UUID a;
        public final UUID b;
        public float length;

        Link(UUID a, UUID b, float length) {
            this.a = a;
            this.b = b;
            this.length = length;
        }

        public UUID other(UUID who) {
            return who.equals(a) ? b : a;
        }
    }

    private final List<Link> links = new ArrayList<>();
    private final Map<UUID, Link> byPlayer = new HashMap<>();

    public static TetherData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TetherData::new, TetherData::load, null), NAME);
    }

    @Nullable
    public Link get(UUID player) {
        return byPlayer.get(player);
    }

    public List<Link> links() {
        return Collections.unmodifiableList(links);
    }

    public Link link(UUID a, UUID b, float length) {
        unlink(a);
        unlink(b);
        Link l = new Link(a, b, length);
        links.add(l);
        byPlayer.put(a, l);
        byPlayer.put(b, l);
        setDirty();
        return l;
    }

    @Nullable
    public Link unlink(UUID player) {
        Link l = byPlayer.get(player);
        if (l == null) return null;
        links.remove(l);
        byPlayer.remove(l.a);
        byPlayer.remove(l.b);
        setDirty();
        return l;
    }

    private static TetherData load(CompoundTag tag, HolderLookup.Provider registries) {
        TetherData data = new TetherData();
        ListTag list = tag.getList("links", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Link l = new Link(c.getUUID("a"), c.getUUID("b"), c.getFloat("length"));
            data.links.add(l);
            data.byPlayer.put(l.a, l);
            data.byPlayer.put(l.b, l);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Link l : links) {
            CompoundTag c = new CompoundTag();
            c.putUUID("a", l.a);
            c.putUUID("b", l.b);
            c.putFloat("length", l.length);
            list.add(c);
        }
        tag.put("links", list);
        return tag;
    }
}
