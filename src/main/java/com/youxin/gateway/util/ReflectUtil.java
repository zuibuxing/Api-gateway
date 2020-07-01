package com.youxin.gateway.util;

import java.lang.reflect.Field;

public class ReflectUtil {
    private static final int SYNTHETIC = 0x00001000;
    private static final int FINAL = 0x00000010;
    private static final int SYNTHETIC_AND_FINAL = SYNTHETIC | FINAL;

    private static boolean checkModifier(int mod) {
        return (mod & SYNTHETIC_AND_FINAL) == SYNTHETIC_AND_FINAL;
    }

    /**
     * 获取外部类
     *
     * @param target
     * @return
     * @throws NoSuchFieldException
     */
    public static Object getExternalClass(Object target) {
        try {
            return getField(target, null, null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getField(Object target, String name, Class classCache) throws NoSuchFieldException {
        if (classCache == null) {
            classCache = target.getClass();
        }
        if (name == null || name.isEmpty()) {
            name = "this$0";
        }
        Field field = classCache.getDeclaredField(name);
        field.setAccessible(true);
        if (checkModifier(field.getModifiers())) {
            try {
                return field.get(target);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return getField(target, name + "$", classCache);
    }
}
