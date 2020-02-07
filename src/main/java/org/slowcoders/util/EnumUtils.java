package org.slowcoders.util;

import com.google.common.collect.ImmutableSet;

import java.util.EnumSet;
import java.util.Set;

public class EnumUtils {

    public static <E extends Enum<E>> E findEnum(Class<E> enumType, String name) {
        E[] enums = enumType.getEnumConstants();
        for (E e : enums) {
            if (e.name().equals(name)) {
                return e;
            }
        }
        return null;
    }

    public static <E extends Enum<E>> E toEnum(Class<E> enumType, int idx) throws ArrayIndexOutOfBoundsException {
        E[] enumConstants = enumType.getEnumConstants();
        return enumConstants[idx];
    }

    public static <E extends Enum<E>> E toEnum(Class<E> enumType, String name) throws IllegalArgumentException {
        E e = findEnum(enumType, name);
        if (e == null) {
            throw new IllegalArgumentException(name + " is not found in " + enumType);
        }
        return e;
    }

    public static <E extends Enum<E>> Set<E> toEnumSet(Class<E> enumType, String[] names, boolean isImmutable) {
        if (names == null) {
            return EnumSet.noneOf(enumType);
        }

        E[] enumConstants = enumType.getEnumConstants();
        EnumSet<E> v = EnumSet.noneOf(enumType);
        for (E e : enumConstants) {
            String enumName = e.name();
            for (String name : names) {
                if (enumName.equals(name)) {
                    v.add(e);
                    break;
                }
            }
        }
        return isImmutable ? ImmutableSet.copyOf(v) : v;
    }

    public static <E extends Enum<E>> EnumSet<E> toEnumSet(Class<E> enumType, E e){
        if (e == null) {
            return EnumSet.noneOf(enumType);
        }

        EnumSet<E> v = EnumSet.noneOf(enumType);
        v.add(e);
        return v;
    }

    public static <E extends Enum<E>> EnumSet<E> toEnumSet(Class<E> enumType, E[] enums) {
        if (enums == null) {
            return EnumSet.noneOf(enumType);
        }

        EnumSet<E> v = EnumSet.noneOf(enumType);
        for (E e : enums) {
            v.add(e);
        }
        return v;
    }

    public static <E extends Enum<E>> Set<E> bitsToEnumSet(Class<E> enumType, long bits, boolean isImmutable) throws ArrayIndexOutOfBoundsException {
        EnumSet<E> v = EnumSet.noneOf(enumType);
        E[] enums = enumType.getEnumConstants();
        Debug.Assert(enums.length <= 64);

        for (int ordinal = 0; bits != 0; bits >>>= 1, ordinal ++) {
            if ((bits & 1) != 0) {
                Debug.Assert(ordinal < enums.length);
                E e = enums[ordinal];
                v.add(e);
            }
        }

        return isImmutable ? ImmutableSet.copyOf(v) : v;
    }


    public static long toBitSet(EnumSet<? extends Enum> enumSet) {
        long bits = 0;
        for (Enum e : enumSet) {
            long bit = 1L << e.ordinal();
            bits |= bit;
        }
        return bits;
    }

    public static String[] toStringArray(EnumSet<? extends Enum> enumSet) {
        String[] names = new String[enumSet.size()];
        int idx = 0;
        for (Enum e : enumSet) {
            names[idx++] = e.name();
        }
        return names;
    }
}
