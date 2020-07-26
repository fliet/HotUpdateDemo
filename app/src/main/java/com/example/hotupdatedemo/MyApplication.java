package com.example.hotupdatedemo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * @author wp
 * @date 2018/7/11
 */

public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        File apk = new File(getCacheDir() + "/classes.dex");
        if (apk.exists()) {
            try {

                /*
                    通过ClassLoader + 反射，获取DexPathList对象
                    再通过DexPathList对象获取数组dexElements
                 */
                ClassLoader classLoader = getClassLoader();
                Class loaderClass = BaseDexClassLoader.class;
                Field pathListField = loaderClass.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathListObject = pathListField.get(classLoader);
                Class pathListClass = pathListObject.getClass();
                Field dexElementsField = pathListClass.getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                Object dexElementsObject = dexElementsField.get(pathListObject);

                 /*
                    创建新数组
                    通过反射 + PathClassLoader获取增量.dex文件加载后的数组
                    获取当前app，.dex文件加载后的数组
                 */
                PathClassLoader newClassLoader = new PathClassLoader(apk.getPath(), classLoader);
                Object newPathListObject = pathListField.get(newClassLoader);
                Object newDexElementsObject = dexElementsField.get(newPathListObject);
                int oldLength = Array.getLength(dexElementsObject);
                int newLength = Array.getLength(newDexElementsObject);
                Object concatDexElementsObject = Array.newInstance(dexElementsObject.getClass().getComponentType(), oldLength + newLength);

                /*
                    将两个数组中的元素复制到新数组中
                 */
                for (int i = 0; i < newLength; i++) {
                    Array.set(concatDexElementsObject, i, Array.get(newDexElementsObject, i));
                }
                for (int i = 0; i < oldLength; i++) {
                    Array.set(concatDexElementsObject, newLength + i, Array.get(dexElementsObject, i));
                }

                dexElementsField.set(pathListObject, concatDexElementsObject);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
