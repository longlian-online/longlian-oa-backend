package online.longlian.generator.internal;

import online.longlian.common.annotation.ModelEnum;
import online.longlian.common.annotation.ModelEnums;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EnumProcessor {

    private static final String ENUM_PACKAGE = "online.longlian.common.enumeration";

    public static Map<EnumFieldMeta, ModelEnumMeta> scanAndPrintModelEnums() throws IOException, ClassNotFoundException {
        var enumMap = new LinkedHashMap<EnumFieldMeta, ModelEnumMeta>();
        List<Class<?>> classes = getClasses();
        for (Class<?> clazz : classes) {
            if (clazz.isEnum()) {
                {
                    ModelEnum modelEnum = clazz.getAnnotation(ModelEnum.class);
                    if (modelEnum != null) {
                        enumMap.put(new EnumFieldMeta(modelEnum.model(), modelEnum.field()), new ModelEnumMeta(clazz.getSimpleName(), clazz.getName()));
                    }
                }

                ModelEnums modelEnums = clazz.getAnnotation(ModelEnums.class);
                if (modelEnums!=null && modelEnums.value() != null && modelEnums.value().length > 0) {
                    for (var modelEnum : modelEnums.value()) {
                        assert modelEnum != null;
                        enumMap.put(new EnumFieldMeta(modelEnum.model(), modelEnum.field()), new ModelEnumMeta(clazz.getSimpleName(), clazz.getName()));
                    }
                }
            }
        }

        return enumMap;
    }

    private static List<Class<?>> getClasses() throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = ENUM_PACKAGE.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        Set<String> classNames = new LinkedHashSet<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("jar".equals(resource.getProtocol())) {
                collectJarClasses((JarURLConnection) resource.openConnection(), path, classNames);
            } else {
                for (Class<?> clazz : findClasses(new File(resource.getFile()), ENUM_PACKAGE)) {
                    classNames.add(clazz.getName());
                }
            }
        }
        List<Class<?>> classes = new ArrayList<>();
        for (String className : classNames) {
            classes.add(Class.forName(className));
        }
        return classes;
    }

    private static void collectJarClasses(JarURLConnection connection, String path, Set<String> classNames) throws IOException {
        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path + "/") && name.endsWith(".class") && !name.contains("$")) {
                    classNames.add(name.substring(0, name.length() - 6).replace('/', '.'));
                }
            }
        }
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }
}
