package com.quickblox.poiuy.utils.helpers;

import android.content.Context;

import com.flurry.android.FlurryAgent;
import com.quickblox.auth.session.QBSettings;
import com.quickblox.users.model.QBUser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tereha on 03.03.16.
 */
public class FlurryAnalyticsHelper {
    public static void pushAnalyticsData(Context context) {
        // init Flurry
        FlurryAgent.setLogEnabled(true);

        // my key XWY3TV6J44QKWDRJQQP4
        FlurryAgent.init(context, "XWY3TV6J44QKWDRJQQP4"); // testing->P8NWM9PBFCK2CWC8KZ59---

        Map<String, String> params = new HashMap<>();

        //param keys and values have to be of String type
        params.put("app_id", QBSettings.getInstance().getApplicationId());
        params.put("chat_endpoint", QBSettings.getInstance().getChatEndpoint());

        //up to 10 params can be logged with each event
        FlurryAgent.logEvent("connect_to_chat", params);
    }


    public static void passingUserData(QBUser qbUser)
    {
        Map<String, String> params = new HashMap<>();

        //param keys and values have to be of String type
        if(qbUser.getId() !=null)
            params.put("user_id", ""+qbUser.getId());
        if(qbUser.getFullName() !=null)
            params.put("user_name", ""+qbUser.getFullName());
        if(qbUser.getEmail() !=null)
            params.put("user_email", ""+qbUser.getEmail());
        if(qbUser.getPassword() !=null)
            params.put("user_password",""+qbUser.getPassword());


        //up to 10 params can be logged with each event
        FlurryAgent.logEvent("user_data", params);
    }
}