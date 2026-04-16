package online.longlian.generator.internal;

import online.longlian.app.common.annotation.ModelEnum;
import online.longlian.app.common.annotation.ModelEnums;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

public class EnumProcessor {

    private static final String ENUM_PACKAGE = "online.longlian.app.common.enumeration";

    public static HashMap<EnumFieldMeta, ModelEnumMeta> scanAndPrintModelEnums() throws IOException, ClassNotFoundException {
        var enumMap = new HashMap<EnumFieldMeta, ModelEnumMeta>();
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
        String path = EnumProcessor.ENUM_PACKAGE.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, EnumProcessor.ENUM_PACKAGE));
        }
        return classes;
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
