package com.example.module;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainModule implements IXposedHookLoadPackage {
    private static final String TAG = "ApexMasterEngine";
    
    private static final int ECPM_MIN = 37000;
    private static final int ECPM_MAX = 63000;
    
    private static final Set<String> HOOKED_CACHE = ConcurrentHashMap.newKeySet();
    private static final Random RANDOM = new Random();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName == null || 
            lpparam.packageName.startsWith("android") || 
            lpparam.packageName.startsWith("com.android") ||
            lpparam.packageName.startsWith("com.google") ||
            lpparam.packageName.equals("system") ||
            lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        XposedBridge.log(TAG + " 🛡️ Apex Master 引擎已安全挂载至目标: " + lpparam.packageName);

        try {
            XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (Boolean.TRUE.equals(param.getResult())) {
                        File file = (File) param.thisObject;
                        if (file != null) {
                            String path = file.getAbsolutePath().toLowerCase();
                            if (path.contains("su") || path.contains("magisk") || 
                                path.contains("kernelsu") || path.contains("zygisk") || 
                                path.contains("lsposed") || path.contains("frida") ||
                                path.contains("xposed") || path.contains("riru") ||
                                path.contains("shadowhook") || path.contains("whale")) {
                                param.setResult(false);
                            }
                        }
                    }
                }
            });

            Class<?> systemProperties = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(systemProperties, "get", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof String) {
                        String key = (String) param.args[0];
                        if ("ro.debuggable".equals(key)) param.setResult("0");
                        else if ("ro.secure".equals(key)) param.setResult("1");
                        else if ("ro.kernel.qemu".equals(key)) param.setResult("0");
                        else if ("ro.adb.secure".equals(key)) param.setResult("1");
                    }
                }
            });
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Class<?> clazz = (Class<?>) param.getResult();
                    if (clazz == null) return;

                    String className = clazz.getName();
                    if (className.startsWith("android.") || className.startsWith("java.") || 
                        className.startsWith("kotlin.") || className.startsWith("androidx.") ||
                        className.startsWith("com.google.") || className.startsWith("dalvik.") ||
                        className.startsWith("com.huawei.") || className.startsWith("com.miui.") ||
                        className.startsWith("com.samsung.")) {
                        return;
                    }

                    String lowerClassName = className.toLowerCase();

                    if (lowerClassName.contains("ad") || lowerClassName.contains("mediation") || 
                        lowerClassName.contains("reward") || lowerClassName.contains("ecpm") || 
                        lowerClassName.contains("bidding") || lowerClassName.contains("price") ||
                        lowerClassName.contains("listener") || lowerClassName.contains("callback") ||
                        lowerClassName.contains("pangle") || lowerClassName.contains("gdt") || 
                        lowerClassName.contains("sigmob") || lowerClassName.contains("topon") ||
                        lowerClassName.contains("ksad") || lowerClassName.contains("baidu") ||
                        lowerClassName.contains("skip") || lowerClassName.contains("countdown") ||
                        lowerClassName.contains("player") || lowerClassName.contains("accel")) {

                        try {
                            Method[] methods = clazz.getDeclaredMethods();
                            if (methods == null) return;

                            for (Method method : methods) {
                                if (method == null) continue;
                                String mName = method.getName().toLowerCase();
                                
                                if (mName.contains("ecpm") || mName.contains("pecpm") || 
                                    mName.contains("price") || mName.contains("bid") ||
                                    mName.contains("revenue") || mName.contains("amount") ||
                                    mName.contains("value") || mName.contains("cost") ||
                                    mName.contains("rate")) {

                                    String methodKey = clazz.getName() + "#" + method.getName();
                                    if (!HOOKED_CACHE.add(methodKey)) continue;

                                    method.setAccessible(true);
                                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam methodParam) throws Throwable {
                                            Object res = methodParam.getResult();
                                            if (res == null) return;

                                            int fakeValue = ECPM_MIN + RANDOM.nextInt(ECPM_MAX - ECPM_MIN + 1);
                                            Class<?> retType = method.getReturnType();

                                            if (retType == double.class || retType == Double.class) {
                                                methodParam.setResult((double) fakeValue);
                                            } else if (retType == float.class || retType == Float.class) {
                                                methodParam.setResult((float) fakeValue);
                                            } else if (retType == long.class || retType == Long.class) {
                                                methodParam.setResult((long) fakeValue);
                                            } else if (retType == int.class || retType == Integer.class) {
                                                methodParam.setResult(fakeValue);
                                            } else if (retType == String.class) {
                                                methodParam.setResult(String.valueOf(fakeValue));
                                            }
                                        }
                                    });
                                }

                                if (mName.contains("countdown") || mName.contains("remain") || 
                                    mName.contains("gettime") || mName.contains("iscompleted") ||
                                    mName.contains("isskip") || mName.contains("canclose")) {

                                    String skipKey = clazz.getName() + "#SK#" + method.getName();
                                    if (!HOOKED_CACHE.add(skipKey)) continue;

                                    method.setAccessible(true);
                                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam methodParam) throws Throwable {
                                            Class<?> retType = method.getReturnType();
                                            String name = method.getName().toLowerCase();

                                            if (name.contains("countdown") || name.contains("remain") || name.contains("time")) {
                                                if (retType == int.class || retType == Integer.class) methodParam.setResult(0);
                                                else if (retType == long.class || retType == Long.class) methodParam.setResult(0L);
                                            } else if (retType == boolean.class || retType == Boolean.class) {
                                                methodParam.setResult(true);
                                            }
                                        }
                                    });
                                }

                                if (mName.contains("onreward") || mName.contains("onadcomplete") || 
                                    mName.contains("onvideocomplete") || mName.contains("onrewarded") ||
                                    mName.contains("onfinished") || mName.contains("onsuccess")) {

                                    String cbKey = clazz.getName() + "#CB#" + method.getName();
                                    if (!HOOKED_CACHE.add(cbKey)) continue;

                                    method.setAccessible(true);
                                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam methodParam) throws Throwable {
                                        }
                                    });
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}
    }
}
