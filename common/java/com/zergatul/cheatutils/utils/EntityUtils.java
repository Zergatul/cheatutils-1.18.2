package com.zergatul.cheatutils.utils;

import com.zergatul.cheatutils.common.Registries;
import com.zergatul.cheatutils.mixins.common.accessors.EntityTypeAccessor;
import com.zergatul.cheatutils.wrappers.ClassRemapper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;

public class EntityUtils {

    private static final Logger logger = LogManager.getLogger(EntityUtils.class);

    private static List<EntityInfo> classes;
    private static Map<String, EntityInfo> classMap;

    public static List<EntityInfo> getEntityClasses() {
        if (classes == null) {
            loadEntityClasses();
        }
        return classes;
    }

    public static EntityInfo getEntityClass(String name) {
        if (classMap == null) {
            loadEntityClasses();
        }
        return classMap.get(name);
    }

    private static synchronized void loadEntityClasses() {
        if (classes != null) {
            return;
        }

        List<EntityInfo> finalClasses = new ArrayList<>();
        HashSet<EntityInfo> set = new HashSet<>();

        EntityInfo info1 = new EntityInfo(Player.class, "minecraft:player");
        finalClasses.add(info1);
        set.add(info1);

        EntityInfo info2 = new EntityInfo(LocalPlayer.class);
        finalClasses.add(info2);
        set.add(info2);

        EntityInfo info3 = new EntityInfo(RemotePlayer.class);
        finalClasses.add(info3);
        set.add(info3);

        Registries.ENTITY_TYPES.getValues().stream().map(et -> {
            if (et == EntityType.PLAYER) {
                return null;
            }

            String key = com.zergatul.cheatutils.common.Registries.ENTITY_TYPES.getKey(et).toString();
            EntityType.EntityFactory<?> factory = ((EntityTypeAccessor) et).getFactory_CU();
            Class<?> entityClass;
            try {
                try {
                    Entity entity = factory.create(null, null);
                    entityClass = entity.getClass();
                } catch (Throwable throwable) {
                    StackTraceElement element = findEntityConstructor(throwable);
                    entityClass = Class.forName(element.getClassName());
                }

                EntityInfo info = new EntityInfo(entityClass, Registries.ENTITY_TYPES.getKey(et).toString());
                set.add(info);
                return info;
            } catch (Throwable throwable) {
                logger.warn("Create entity by EntityType {} failed.", key, throwable);
                return null;
            }
        }).filter(Objects::nonNull).forEach(finalClasses::add);

        Set<Class<?>> interfaces = new HashSet<>();

        finalClasses.forEach(ei -> {
            forEachInterface(ei.clazz, interfaces::add);

            Class<?> clazz = ei.clazz.getSuperclass();
            while (Entity.class.isAssignableFrom(clazz)) {
                try {
                    EntityInfo baseInfo = new EntityInfo(clazz);
                    set.add(baseInfo);
                } catch (Exception ex) {
                    logger.warn("Cannot create EntityInfo for base class {}.", clazz.getName(), ex);
                    continue;
                }
                clazz = clazz.getSuperclass();
            }
        });

        classes = new ArrayList<>();
        classes.addAll(set);
        for (Class<?> iface : interfaces) {
            try {
                classes.add(new EntityInfo(iface));
            } catch (Exception ex) {
                logger.warn("Cannot create EntityInfo for interface {}.", iface.getName(), ex);
            }
        }

        classes = classes.stream().sorted((i1, i2) -> i1.simpleName.compareToIgnoreCase(i2.simpleName)).toList();

        classMap = new HashMap<>(classes.size());
        for (EntityInfo info: classes) {
            classMap.put(info.clazz.getName(), info);
        }
    }

    private static StackTraceElement findEntityConstructor(Throwable throwable) {
        StackTraceElement[] elements = throwable.getStackTrace();
        int index = -1;
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].getClassName().startsWith("com.zergatul.cheatutils")) {
                index = i;
                break;
            }
        }
        if (index <= 0) {
            throw new IllegalStateException("Cannot process stack trace.");
        }

        while (index > 0) {
            index--;
            StackTraceElement element = elements[index];
            if (element.getMethodName().equals("<init>")) {
                return element;
            }
        }

        throw new IllegalStateException("Constructor call expected on stack trace.");
    }

    private static void forEachInterface(Class<?> clazz, Consumer<Class<?>> consumer) {
        while (clazz != Entity.class) {
            Arrays.stream(clazz.getInterfaces()).forEach(consumer);
            clazz = clazz.getSuperclass();
        }
    }

    public static class EntityInfo {

        public Class<?> clazz;
        public boolean isInterface;
        public String simpleName;
        public List<Class<?>> baseClasses;
        public List<Class<?>> interfaces;
        public String id;

        public EntityInfo(Class<?> clazz) {
            this(clazz, null);
        }

        public EntityInfo(Class<?> clazz, String id) {
            if (clazz.isInterface()) {
                this.clazz = clazz;
                simpleName = getSimpleName(clazz);
                isInterface = true;
            } else {
                if (!Entity.class.isAssignableFrom(clazz)) {
                    throw new IllegalStateException("Not supported.");
                }

                this.clazz = clazz;
                simpleName = getSimpleName(clazz);

                this.id = id;

                baseClasses = new ArrayList<>();
                while (clazz != Entity.class) {
                    clazz = clazz.getSuperclass();
                    baseClasses.add(clazz);
                }

                interfaces = new ArrayList<>();
                forEachInterface(this.clazz, iface -> interfaces.add(iface));
            }
        }

        @Override
        public int hashCode() {
            return clazz.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EntityInfo ei) {
                return ei.clazz == clazz;
            } else {
                return false;
            }
        }

        private String getSimpleName(Class<?> clazz) {
            String rawName = ClassRemapper.fromObf(clazz.getName());
            int index = rawName.lastIndexOf('.');
            if (index < 0) {
                return rawName;
            } else {
                return rawName.substring(index + 1);
            }
        }
    }
}