package org.slowcoders.io.serialize;

import org.slowcoders.util.Debug;
import org.slowcoders.util.ClassUtils;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

public final class IOAdapterLoader {


    private static final IOAdapterLoader instance;

    private final ArrayList<InheritableAdapter> inheritableAdapters = new ArrayList<>();

    private final HashMap<Object, IOAdapter> adapters = new HashMap<>();

    private final ArrayList<FactoryResolver> factories = new ArrayList<>();

    static {
        instance = new IOAdapterLoader();
        instance.init();
    }

    protected IOAdapterLoader() { }

    protected void init() {
        loadDefaultAdapters(adapters);
        addInheritableFactory(Iterable.class, IOAdapters._Collection.factory);
        addInheritableFactory(Set.class, IOAdapters._Set.factory);
        addInheritableFactory(Map.class, IOAdapters._Map.factory);
    }

    public void addInheritableFactory(Class<?> topClass, IOAdapterFactory factory) {
        this.addFactoryResolver(new FactoryResolver(topClass, factory));
    }

    public void addFactoryResolver(FactoryResolver resolver) {
        this.factories.add(resolver);
    }

    public static <T, E> IOAdapter<T, E> registerDefaultAdapter(Class<T> type, IOAdapter<T, E> adapter) {
        return instance.registerAdapter(type, adapter);
    }

    public <T, E> IOAdapter<T, E> registerAdapter(Class<T> type, IOAdapter<T, E> adapter) {
        synchronized (adapters) {
            if ((type.getModifiers() & Modifier.FINAL) != 0) {
                IOAdapter old = adapters.put(type, adapter);
                if (Debug.DEBUG && old != null) {
                    Debug.wtf("duplicated adapter registering " + type.getName());
                }
            }
            else {
                int index = inheritableAdapters.size();
                int insertIndex = index;
                for (; --index > 0;) {
                    InheritableAdapter entry = inheritableAdapters.get(index);
                    if (type.isAssignableFrom(entry.topClass)) {
                        if (entry.topClass == type) {
                            Debug.wtf("duplicated adapter registering " + type.getName());
                        }
                        index = insertIndex;
                    }
                }
                inheritableAdapters.add(insertIndex, new InheritableAdapter(type, adapter));
            }
        }
        return adapter;
    }

    public static IOAdapter load(Type type) {
        return instance.loadAdapter(type);
    }


    private IOAdapter loadAdapter(Type type) {
        IOAdapter tr;


        Class<?> clazz = ClassUtils.toClass(type);

        tr = adapters.get(type);
        if (tr == null) {
            tr = adapters.get(type);
        }
        if (tr != null) {
            return tr;
        }


        IOCtrl ioctrl = clazz.getAnnotation(IOCtrl.class);
        if (ioctrl != null && ioctrl.adapter() != null) {
            tr = IOAdapter.getAdapterInstance(ioctrl.adapter());
            if (tr != null) {
                adapters.put(type, tr);
                return tr;
            }
        }
        else {
            try {
                /**
                 * IOAdapter 를 포함하는 Class를 초기화하기 위해
                 * Static 필드들을 access 한다.
                 */
                String cname = clazz.getName();
                Class.forName(cname);
                // adapter 가 등록되었을 수 있다. 다시 검사.
                tr = adapters.get(type);
                if (tr != null) {
                    return tr;
                }
            }
            catch (Exception e) {
                Debug.wtf(e);
            }
        }

        tr = findInheritableAdapter(clazz, adapters);
        if (tr != null) {
            return tr;
        }

        if (clazz.isEnum()) {
            tr = new IOAdapters._Enum.Adapter(clazz);
        } else if (clazz == EnumSet.class) {
            tr = new IOAdapters._EnumSet.Adapter(ClassUtils.getFirstGenericParameter(type));
        } else if (clazz.isArray()) {
            tr = IOAdapters._Array.factory.makeAdapter(clazz.getComponentType(), this);
        }

        if (tr != null) {
            adapters.put(clazz, tr);
            return tr;
        }

        for (int i = factories.size(); --i >= 0; ) {
            FactoryResolver resolver = factories.get(i);
            tr = resolver.createAdapter(clazz, type, this);
            if (tr != null) {
                break;
            }
        }

        if (tr == null) {
            tr = createAdapter(clazz);
        }
        adapters.put(type, tr);
        return tr;
    }

    private IOAdapter findInheritableAdapter(Class<?> clazz, HashMap<Object, IOAdapter> localAdapters) {
        IOAdapter tr = null;
        for (int i = inheritableAdapters.size(); --i > 0; ) {
            InheritableAdapter entry = inheritableAdapters.get(i);
                if ((tr = entry.getAdapter(clazz)) != null) {
                    if (localAdapters != null) {
                        localAdapters.put(clazz, tr);
                    }
                break;
            }
        }
        return tr;
    }

//    protected IOAdapter findDefaultAdapter(Class<?> clazz, HashMap<Object, IOAdapter> localAdapters) {
//        IOAdapter tr;
//        synchronized (adapters) {
//            tr = adapters.get(clazz);
//            if (tr == null) {
//                tr = findDefaultInheritableAdapter(clazz, localAdapters);
//            }
//        }
//        return tr;
//    }


//    private Factory makeFactory(Class<? extends Factory> factoryType) {
//        synchronized (factories) {
//            Factory factory = factories.get(factoryType);
//            if (factory == null) {
//                try {
//                    factory = ClassUtils.newInstance(factoryType);
//                } catch (Exception e) {
//                    NPDebug.wtf(e);
//                }
//            }
//            return factory;
//        }
//    }

    protected IOAdapter<?, ?> createAdapter(Class<?> field_t) {
        return IOAdapters._Object.adapter;
    }

//    private static class CustomAdapterSearchKey {
//        Type type;
//        Class<? extends IOAdapterFactory> factoryType;
//
//        CustomAdapterSearchKey() {}
//
//        CustomAdapterSearchKey(CustomAdapterSearchKey other) {
//            type = other.type;
//            factoryType = other.factoryType;
//        }
//
//        public int hashCode() {
//            return type.hashCode();
//        }
//
//        public boolean equals(Object o) {
//            if (o instanceof CustomAdapterSearchKey) {
//                CustomAdapterSearchKey key = (CustomAdapterSearchKey)o;
//                return (key.type == this.type && key.factoryType == this.factoryType);
//            }
//            return false;
//        }
//    }

    static class FactoryResolver {
        final Class<?> topClass;
        final IOAdapterFactory factory;

        public FactoryResolver(Class<?> topClass, IOAdapterFactory factory) {
            this.topClass = topClass;
            this.factory = factory;
        }

        public IOAdapter createAdapter(Class<?> clazz, Type type, IOAdapterLoader adapterLoader) {
            if (topClass.isAssignableFrom(clazz)) {
                return factory.createAdapter(type, adapterLoader);
            }
            return null;
        }
    }

    static class InheritableAdapter {
        final Class<?> topClass;
        final IOAdapter adapter;

        public InheritableAdapter(Class<?> topClass, IOAdapter adapter) {
            this.topClass = topClass;
            this.adapter = adapter;
        }

        public IOAdapter getAdapter(Class<?> clazz) {
            if (topClass.isAssignableFrom(clazz)) {
                return adapter;
            }
            return null;
        }
    }


    protected void loadDefaultAdapters(HashMap<Object, IOAdapter> adapters) {

        adapters.put(boolean.class, new IOAdapters._Boolean.Adapter(true));
        adapters.put(byte.class, new IOAdapters._Byte.Adapter(true));
        adapters.put(char.class, new IOAdapters._Char.Adapter(true));
        adapters.put(short.class, new IOAdapters._Short.Adapter(true));
        adapters.put(int.class, new IOAdapters._Int.Adapter(true));
        adapters.put(long.class, new IOAdapters._Long.Adapter(true));
        adapters.put(float.class, new IOAdapters._Float.Adapter(true));
        adapters.put(double.class, new IOAdapters._Double.Adapter(true));

        adapters.put(Boolean.class, new IOAdapters._Boolean.Adapter(false));
        adapters.put(Byte.class, new IOAdapters._Byte.Adapter(false));
        adapters.put(Character.class, new IOAdapters._Char.Adapter(false));
        adapters.put(Short.class, new IOAdapters._Short.Adapter(false));
        adapters.put(Integer.class, new IOAdapters._Int.Adapter(false));
        adapters.put(Long.class, new IOAdapters._Long.Adapter(false));
        adapters.put(Float.class, new IOAdapters._Float.Adapter(false));
        adapters.put(Double.class, new IOAdapters._Double.Adapter(false));

        adapters.put(String.class, IOAdapters._String.adapter);
        adapters.put(Number.class, IOAdapters._Number.adapter);


        adapters.put(boolean[].class, IOAdapters._BooleanArray.adapter);
        adapters.put(byte[].class, IOAdapters._ByteArray.adapter);
        adapters.put(char[].class, IOAdapters._CharArray.adapter);
        adapters.put(short[].class, IOAdapters._ShortArray.adapter);
        adapters.put(int[].class, IOAdapters._IntArray.adapter);
        adapters.put(long[].class, IOAdapters._LongArray.adapter);
        adapters.put(float[].class, IOAdapters._FloatArray.adapter);
        adapters.put(double[].class, IOAdapters._DoubleArray.adapter);

        PredefinedAdapters.registerAdapters();

    }


}