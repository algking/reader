package com.sismics.reader;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender.Method;
import org.acra.sender.HttpSender.Type;
import org.json.JSONObject;

import android.app.Application;

import com.androidquery.callback.BitmapAjaxCallback;
import com.sismics.reader.model.application.ApplicationContext;
import com.sismics.reader.util.PreferenceUtil;

/**
 * Main application.
 * 
 * @author bgamard
 */
@ReportsCrashes(formKey = "",
        httpMethod = Method.PUT,
        reportType = Type.JSON,
        formUri = "http://acralyzer.sismics.com/reader-report",
        formUriBasicAuthLogin = "reporter",
        formUriBasicAuthPassword = "jOS9ezJR",
        mode = ReportingInteractionMode.TOAST,
        forceCloseDialogAfterToast = true,
        resToastText = R.string.crash_toast_text)
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        ACRA.init(this);
        
        // Fetching /user/info from cache
        String jsonStr = PreferenceUtil.getStringPreference(getApplicationContext(), PreferenceUtil.PREF_CACHED_USER_INFO_JSON);
        try {
            JSONObject json = new JSONObject(jsonStr);
            ApplicationContext.getInstance().setUserInfo(getApplicationContext(), json);
        } catch (Exception e) {
            // Should not happen
            PreferenceUtil.setCachedUserInfoJson(getApplicationContext(), null);
        }
        
        super.onCreate();
    }
    
    @Override
    public void onLowMemory() {
        BitmapAjaxCallback.clearCache();
    }
}
