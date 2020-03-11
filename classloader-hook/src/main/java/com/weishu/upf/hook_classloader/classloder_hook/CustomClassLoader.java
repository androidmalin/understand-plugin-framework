package com.weishu.upf.hook_classloader.classloder_hook;

import dalvik.system.DexClassLoader;

/**
 * 自定义的ClassLoader, 用于加载"插件"的资源和代码
 * <p>
 * 我们的这个CustomClassLoader非常简单,直接继承了DexClassLoader,什么都没有做;
 * 当然这里可以直接使用DexClassLoader,这里重新创建一个类是为了更有区分度;以后也可以通过修改这个类实现对于类加载的控制
 *
 * @author weishu
 * @date 16/3/29
 */
public class CustomClassLoader extends DexClassLoader {

    public CustomClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
    }
}
