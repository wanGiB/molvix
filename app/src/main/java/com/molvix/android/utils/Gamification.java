package com.molvix.android.utils;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.molvix.android.preferences.AppPrefs;
import com.molvix.android.ui.activities.MainActivity;

public class Gamification {
    public static void displayCoinEssence(Context context, String title){
        AlertDialog.Builder coinEssenceDialogBuilder = new AlertDialog.Builder(context);
        coinEssenceDialogBuilder.setTitle(UiUtils.fromHtml(title));
        int availableCoins = AppPrefs.getDownloadCoins();
        String contentMsg= "Downloads on Molvix happen using <b>Download coins</b>.\n\n\nYou currently have <b>"+availableCoins+" coins </b>.\n\n\nTo get more coins, all you have to do is to tap on the "+(" <b>WATCH VIDEO AD</b> button below to ")+"watch a few seconds video ad.";
        coinEssenceDialogBuilder.setMessage(UiUtils.fromHtml(contentMsg));
        coinEssenceDialogBuilder.setPositiveButton("WATCH VIDEO AD", (dialog, which) -> {
            dialog.dismiss();
            if (context instanceof MainActivity){
                MainActivity scope = (MainActivity) context;
                scope.loadRewardedVideoAd();
            }
        });
        coinEssenceDialogBuilder.setNegativeButton("CLOSE", (dialog, which) -> dialog.dismiss());
        coinEssenceDialogBuilder.create();
    }

}
