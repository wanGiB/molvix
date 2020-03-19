package com.molvix.android.managers;

import com.molvix.android.companions.AppConstants;
import com.molvix.android.utils.MolvixLogger;

import java.util.Stack;

public class DownloadedItemsPositionsManager {
    public static void enquePosition(int position) {
        AppConstants.downloadedVideoItemsPositionsStack.push(position);
        MolvixLogger.d("PositionsStack", AppConstants.downloadedVideoItemsPositionsStack.toString());
    }

    public static void popLastPosition() {
        Stack<Integer> positionsStack = AppConstants.downloadedVideoItemsPositionsStack;
        if (!positionsStack.isEmpty()) {
            AppConstants.downloadedVideoItemsPositionsStack.pop();
            MolvixLogger.d("PositionsStack", AppConstants.downloadedVideoItemsPositionsStack.toString());
        }
    }

    public static int getLastPosition() {
        Stack<Integer> positionsStack = AppConstants.downloadedVideoItemsPositionsStack;
        if (!positionsStack.isEmpty()) {
            return AppConstants.downloadedVideoItemsPositionsStack.peek();
        } else {
            return 0;
        }
    }
    
}