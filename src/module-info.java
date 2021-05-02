module dorkbox.swtjavafx {
    exports dorkbox.javaFx;
    exports dorkbox.swt;

    requires dorkbox.utilities;

    requires javafx.graphics;

    // 32-bit support was dropped by eclipse since 4.10 (3.108.0 is the oldest that is 32 bit)
    // this must match what your release target and SWT dependency is
     requires org.eclipse.swt.gtk.linux.x86_64;
//     requires static org.eclipse.swt.win32.win32.x86_64;
//     requires static org.eclipse.swt.cocoa.macosx.x86_64;

    requires java.base;
}
