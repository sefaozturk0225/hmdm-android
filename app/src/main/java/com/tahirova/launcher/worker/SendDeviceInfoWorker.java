package com.tahirova.launcher.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tahirova.launcher.Const;
import com.tahirova.launcher.helper.SettingsHelper;
import com.tahirova.launcher.json.DeviceInfo;
import com.tahirova.launcher.server.ServerService;
import com.tahirova.launcher.server.ServerServiceKeeper;
import com.tahirova.launcher.util.DeviceInfoProvider;

import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class SendDeviceInfoWorker extends Worker {

    private static final int SEND_DEVICE_INFO_PERIOD_MINS = 15;

    private static final String WORK_TAG_DEVICEINFO = "com.tahirova.launcher.WORK_TAG_DEVICEINFO";

    private Context context;
    private SettingsHelper settingsHelper;

    public SendDeviceInfoWorker(
            @NonNull final Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        settingsHelper = SettingsHelper.getInstance(context);
    }

    @Override
    // This is running in a background thread by WorkManager
    public Result doWork() {
        if (settingsHelper == null || settingsHelper.getConfig() == null) {
            return Result.failure();
        }

        DeviceInfo deviceInfo = DeviceInfoProvider.getDeviceInfo(context, true, true);

        ServerService serverService = ServerServiceKeeper.getServerServiceInstance(context);
        ServerService secondaryServerService = ServerServiceKeeper.getSecondaryServerServiceInstance(context);
        Response<ResponseBody> response = null;

        try {
            response = serverService.sendDevice(settingsHelper.getServerProject(), deviceInfo).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (response == null) {
                response = secondaryServerService.sendDevice(settingsHelper.getServerProject(), deviceInfo).execute();
            }
            if ( response.isSuccessful() ) {
                SettingsHelper.getInstance(context).setExternalIp(response.headers().get(Const.HEADER_IP_ADDRESS));
                return Result.success();
            }
        }
        catch ( Exception e ) { e.printStackTrace(); }

        return Result.failure();
    }

    public static void scheduleDeviceInfoSending(Context context) {
        // Periodic background sending disabled: device info is sent only at app start / manual sync
    }
}
