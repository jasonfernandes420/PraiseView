package com.praiseview.util;

import com.sun.jna.platform.win32.Kernel32;

public final class PowerAwakeService {

    private static final int ES_SYSTEM_REQUIRED = 0x00000001;
    private static final int ES_DISPLAY_REQUIRED = 0x00000002;
    private static final int ES_CONTINUOUS = 0x80000000;

    private static boolean awakeRequested = false;

    private PowerAwakeService() {
    }

    public static synchronized void requestAwakeMode() {
        if (awakeRequested) {
            return;
        }
        if (!isWindows()) {
            AppLogger.log("Power awake mode skipped: non-Windows OS.");
            return;
        }

        int result = Kernel32.INSTANCE.SetThreadExecutionState(
                ES_CONTINUOUS | ES_SYSTEM_REQUIRED | ES_DISPLAY_REQUIRED
        );

        if (result == 0) {
            AppLogger.log("Failed to enable awake mode (SetThreadExecutionState returned 0).");
            return;
        }

        awakeRequested = true;
        AppLogger.log("Awake mode enabled. System/display sleep prevented while app is running.");
    }

    public static synchronized void releaseAwakeMode() {
        if (!isWindows()) {
            return;
        }

        int result = Kernel32.INSTANCE.SetThreadExecutionState(ES_CONTINUOUS);
        if (result == 0) {
            AppLogger.log("Failed to release awake mode (SetThreadExecutionState returned 0).");
            return;
        }

        awakeRequested = false;
        AppLogger.log("Awake mode released. Default system sleep behavior restored.");
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("win");
    }
}
