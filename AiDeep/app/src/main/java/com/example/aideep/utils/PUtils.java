package com.example.aideep.utils;

import android.content.Context;

import com.kaopiz.kprogresshud.KProgressHUD;

public class PUtils {
    public static KProgressHUD kProgressHUD;

    public static void showLoading(Context context) {
        if (kProgressHUD == null) {
            kProgressHUD = KProgressHUD.create(context)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
//                .setLabel("Please wait")
//                .setDetailsLabel("Downloading data")
                    .setCancellable(true)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.5f);
        }
        kProgressHUD.show();
    }

    public static void hideLoading() {
        if (kProgressHUD != null) {
            kProgressHUD.dismiss();
        }
    }
}
