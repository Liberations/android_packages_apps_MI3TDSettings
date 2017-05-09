package com.guai.mi3setting;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;

public class ForceFastChargePreference extends SwitchPreference {

    private static final String PERSIST_FORCE_FAST_CHARGE = "persist.force_fast_charge";

    // copied from android.os.BatteryManager.EXTRA_MAX_CHARGING_CURRENT;
    private static final String EXTRA_MAX_CHARGING_CURRENT = "max_charging_current";
    private static final String BATTERY_CURRENT_PATH = "/sys/class/power_supply/max170xx_battery/current_now";
    private static final String CHARGE_FULL_PATH = "/sys/class/power_supply/max170xx_battery/charge_full";
    private static final String MAX_CHARGING_CURRENT_PATH1 = "/sys/class/power_supply/ac/current_max";
    private static final String MAX_CHARGING_CURRENT_PATH2 = "/sys/class/power_supply/usb/current_max";

    private static final int MSG_BATTERY_UPDATE = 302;

    private class BatteryStatus {
        final boolean plugged;
        final int maxChargingCurrent;
        BatteryStatus(boolean plugged, int maxChargingCurrent) {
            this.plugged = plugged;
            this.maxChargingCurrent = maxChargingCurrent;
        }
    }
    private BatteryStatus batteryStatus_;

    public ForceFastChargePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        batteryStatus_ = new BatteryStatus(false, 0);

        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if (!(boolean) newValue) {
                    return true;
                }

                new AlertDialog.Builder(getContext())
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setChecked(true);
                                getEditor()
                                        .putBoolean(PERSIST_FORCE_FAST_CHARGE, true)
                                        .apply();
                                updateChargingStatus();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(R.drawable.ic_warning)
                        .setTitle(R.string.force_fast_charge)
                        .setMessage(R.string.force_fast_charge_confirmmessage)
                        .create().show();
                return false;
            }
        });
    }

    void updateChargingStatus() {
        final int batteryCurrent = Helper.readInt(BATTERY_CURRENT_PATH, -1);
        final int chargeFull = Helper.readInt(CHARGE_FULL_PATH, -1);
        int maxChargingCurrent = batteryStatus_.maxChargingCurrent;
        if (maxChargingCurrent == -1) {
            maxChargingCurrent = Helper.readInt(MAX_CHARGING_CURRENT_PATH1, -1);
            if (maxChargingCurrent == -1) {
                maxChargingCurrent = Helper.readInt(MAX_CHARGING_CURRENT_PATH2, -1);
            }
        }

        String max;
        if (batteryStatus_.plugged) {
            max = getContext().getString(R.string.force_fast_charge_charging, maxChargingCurrent / 1000);
        }
        else {
            max = getContext().getString(R.string.force_fast_charge_discharging);
        }

        setSummary(getContext().getString(
                R.string.force_fast_charge_summary,
                batteryCurrent / 1000, max, chargeFull / 1000
        ));
    }

    private final Handler handler_ = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BATTERY_UPDATE:
                    batteryStatus_ = (BatteryStatus) msg.obj;
                    updateChargingStatus();
                    break;
            }
        }
    };

	void updateChargingStatus(Intent intent) {
        final boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        final int maxChargingCurrent = intent.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, -1);

		final Message msg = handler_.obtainMessage(
                MSG_BATTERY_UPDATE,
                new BatteryStatus(plugged, maxChargingCurrent)
        );
		handler_.sendMessage(msg);
	}
}

