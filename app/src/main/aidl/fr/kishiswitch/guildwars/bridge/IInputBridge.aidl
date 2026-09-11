package fr.kishiswitch.guildwars.bridge;
import android.view.KeyEvent;
import android.view.MotionEvent;

interface IInputBridge {
    boolean ping() = 1;
    boolean injectKey(in KeyEvent event, int targetUid) = 2;
    boolean injectMotion(in MotionEvent event, int targetUid) = 3;
    String getLastError() = 4;
    boolean startRelay(String configuration, IBinder lifetime, int targetUid) = 5;
    String updateRelay(boolean skills, boolean vertical, boolean draining) = 6;
    String stopRelay() = 7;
    void destroy() = 16777114;
}
