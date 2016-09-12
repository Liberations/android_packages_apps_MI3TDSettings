package net.tonyliu.mi3setting;

import android.annotation.Nullable;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;


public class BasebandAutoResetter extends Service {

    private static final int RESET_DELAY = 120;

    private final Handler handler_ = new Handler();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static final String KEY_RIL_AUTO_RESET = "ril_auto_rest";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(KEY_RIL_AUTO_RESET , true)) {
            handler_.postDelayed(new Runnable() {
                @Override
                public void run() {
                    String radioVersion = Build.getRadioVersion();
                    BasebandAutoResetter self = BasebandAutoResetter.this;
                    if (radioVersion.length() == 0) {
                        Helper.resetBaseband(self);
                    }
                    self.stopSelf();
                }
            }, RESET_DELAY * 1000);
        }
        return super.onStartCommand(intent, flags, startId);
    }
}