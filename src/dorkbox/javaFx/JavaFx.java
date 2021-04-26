/*
 * Copyright 2016 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.javaFx;


import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.slf4j.LoggerFactory;

import dorkbox.swt.Swt;

/**
 * Utility methods for JavaFX.
 */
public
class JavaFx {
    public final static boolean isLoaded;
    public final static boolean isGtk3;


    static {
        // There is a silly amount of redirection, simply because we have to be able to access JavaFX, but only if it's in use.
        // Since this class is the place other code interacts with, we can use JavaFX stuff if necessary without loading/linking
        // the JavaFX classes by accident

        // We cannot use getToolkit(), because if JavaFX is not being used, calling getToolkit() will initialize it...
        // see: https://bugs.openjdk.java.net/browse/JDK-8090933

        String fullJavaVersion = System.getProperty("java.version", "");
        boolean isJava8 = fullJavaVersion.startsWith("1.") && fullJavaVersion.charAt(2) == '8';

        Class<?> javaFxLoggerClass = AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
            @Override
            public
            Class<?> run() {
                try {
                    return Class.forName("com.sun.javafx.logging.PlatformLogger", true, ClassLoader.getSystemClassLoader());
                } catch (Exception ignored) {
                }
                try {
                    return Class.forName("com.sun.javafx.logging.PlatformLogger", true, Thread.currentThread().getContextClassLoader());
                } catch (Exception ignored) {
                }
                return null;
            }
        });

        boolean isJavaFxLoaded_ = false;
        try {
            if (javaFxLoggerClass != null) {
                // try to avoid using reflection!

                // first, walk the stack to see if any JavaFX classes are used (which indicate that we are running with JavaFX)
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                for (int i = 0; i < stackTrace.length; i++) {
                    if (stackTrace[i].getClassName().contains("javafx")) {
                        isJavaFxLoaded_ = true;
                        break;
                    }
                }

                // this is important to use reflection, because if JavaFX is not being used, calling getToolkit() will initialize it...
                if (!isJavaFxLoaded_) {
                    // stack walking failed, resort to using a property (emit a warning)
                    String usingJavFx = System.getProperty("usingJavaFx", "false");
                    if (!usingJavFx.equals("false")) {
                        // if it is set to ANYTHING other than exactly "false", then we are using javaFX. Skip reflection testing
                        isJavaFxLoaded_ = true;
                    } else {
                        // stack walking and property check failed, resort to using reflection.
                        // WE DON'T always want to do this BECAUSE Java9+ really doesn't like reflection!
                        LoggerFactory.getLogger(JavaFx.class).debug("Using reflection to detect if JavaFX is loaded.\n" +
                                "To avoid using reflection, set the system property \"usingJavaFx\" to \"true\" before your application starts.  " +
                                "For example: System.setProperty(\"usingJavaFx\", \"true\");");

                        Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
                        m.setAccessible(true);
                        ClassLoader cl = ClassLoader.getSystemClassLoader();

                        isJavaFxLoaded_ = (null != m.invoke(cl, "com.sun.javafx.tk.Toolkit")) || (null != m.invoke(cl, "javafx.application.Application"));
                    }
                }
            }
        } catch (Throwable e) {
            LoggerFactory.getLogger(JavaFx.class).debug("Error detecting if JavaFX is loaded", e);
        }

        boolean isJavaFxGtk3_ = false;
        if (isJavaFxLoaded_) {
            // JavaFX Java7,8 is GTK2 only. Java9 can MAYBE have it be GTK3 if `-Djdk.gtk.version=3` is specified
            // see
            // http://mail.openjdk.java.net/pipermail/openjfx-dev/2016-May/019100.html
            // https://docs.oracle.com/javafx/2/system_requirements_2-2-3/jfxpub-system_requirements_2-2-3.htm
            // from the page: JavaFX 2.2.3 for Linux requires gtk2 2.18+.

            // HILARIOUSLY enough, you can use JavaFX + SWT..... And the javaFX GTK version info SHOULD
            // be based on what SWT has loaded

            // https://github.com/teamfx/openjfx-9-dev-rt/blob/master/modules/javafx.graphics/src/main/java/com/sun/glass/ui/gtk/GtkApplication.java

            if (isJava8) {
                // JavaFX from Oracle Java 8 is GTK2 only. Java9 can have it be GTK3 if -Djdk.gtk.version=3 is specified
                // see http://mail.openjdk.java.net/pipermail/openjfx-dev/2016-May/019100.html
                isJavaFxGtk3_ = false;
            } else {
                // Only possible Java9+ (so our case, Java11+ since 9 is no longer available, 11 is officially LTS)
                if (Swt.isLoaded && !Swt.isGtk3) {
                    isJavaFxGtk3_ = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public
                        Boolean run() {
                            String version = System.getProperty("jdk.gtk.version", "2");
                            return "3".equals(version) || version.startsWith("3.");
                        }
                    });
                }
            }
        }

        isLoaded = isJavaFxLoaded_;
        isGtk3 = isJavaFxGtk3_;
    }

    public static
    void dispatch(final Runnable runnable) {
        JavaFxAccess.dispatch(runnable);
    }

    public static
    boolean isEventThread() {
        return JavaFxAccess.isEventThread();
    }
}
