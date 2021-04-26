package dorkbox.swt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

public
class SwtAccess {
    private static Display currentDisplay = null;
    private static Thread currentDisplayThread = null;

    public static
    void init() {
        // we MUST save this on init, otherwise it is "null" when methods are run from the swing EDT.
        currentDisplay = org.eclipse.swt.widgets.Display.getCurrent();
        currentDisplayThread = currentDisplay.getThread();
    }

    static
    boolean isLoadable() {
        return org.eclipse.swt.SWT.isLoadable();
    }

    static
    void onShutdown(final org.eclipse.swt.widgets.Display currentDisplay, final Runnable runnable) {
        // currentDisplay.getShells() must only be called inside the event thread!

        org.eclipse.swt.widgets.Shell shell = currentDisplay.getShells()[0];
        shell.addListener(org.eclipse.swt.SWT.Close, new org.eclipse.swt.widgets.Listener() {
            @Override
            public
            void handleEvent(final org.eclipse.swt.widgets.Event event) {
                runnable.run();
            }
        });
    }

    static
    int getVersion() {
        return SWT.getVersion();
    }


    private static
    int getViaReflection(String clazz) throws InvocationTargetException, IllegalAccessException {
        final Class<?> osClass  = AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
            @Override
            public
            Class<?> run() {
                try {
                    return Class.forName(clazz, true, ClassLoader.getSystemClassLoader());
                } catch (Exception ignored) {
                }

                try {
                    return Class.forName(clazz, true, Thread.currentThread().getContextClassLoader());
                } catch (Exception ignored) {
                }

                return null;
            }
        });

        Method method = AccessController.doPrivileged(new PrivilegedAction<Method>() {
            @Override
            public
            Method run() {
                try {
                    return osClass.getMethod("gtk_major_version");
                } catch (Exception e) {
                    return null;
                }
            }
        });

        // might throw an exception!
        return ((Number)method.invoke(osClass)).intValue();
    }

    /**
     * This is only necessary for linux.
     *
     * @return true if SWT is GTK3. False if SWT is GTK2 or unknown.
     */
    static boolean isGtk3() {
        boolean isLinux = System.getProperty("os.name", "").toLowerCase(Locale.US).startsWith("linux");
        if (!isLinux) {
            return false;
        }

        try {
            return org.eclipse.swt.internal.gtk.GTK.gtk_get_major_version() == 3;
        } catch (Exception e) {
            // this might be an older/different version of SWT! Try reflection.
            try {
                return getViaReflection("org.eclipse.swt.internal.gtk.GTK") == 3;
            } catch (Exception e1) {
                try {
                    return getViaReflection("org.eclipse.swt.internal.gtk.OS") == 3;
                } catch (Exception ignored) {
                }
            }
        }

        return false;
    }

    static
    void dispatch(final Runnable runnable) {
        currentDisplay.syncExec(runnable);
    }

    static
    boolean isEventThread() {
        return Thread.currentThread() == currentDisplayThread;
    }
}
