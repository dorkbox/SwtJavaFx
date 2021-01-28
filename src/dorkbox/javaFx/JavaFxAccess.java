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

/**
 * Utility methods for JavaFX.
 * <p>
 * We use reflection for these methods so that we can compile everything under a version of Java that might not have JavaFX.
 */
public
class JavaFxAccess {
    static
    void dispatch(final Runnable runnable) {
        javafx.application.Platform.runLater(runnable);
    }

    static
    boolean isEventThread() {
        // JAVA 8
        return com.sun.javafx.tk.Toolkit.getToolkit().isFxUserThread();
    }

    public static
    void onShutdown(final Runnable runnable) {
        com.sun.javafx.tk.Toolkit.getToolkit().addShutdownHook(runnable);
    }
}