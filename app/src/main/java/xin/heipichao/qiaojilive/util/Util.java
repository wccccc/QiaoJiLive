package xin.heipichao.qiaojilive.util;

import android.app.Activity;
import android.graphics.Rect;
import android.util.DisplayMetrics;

public class Util {
    public static int getStatusBarHeight(Activity activity) {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }else{
            DisplayMetrics dm = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            Rect outRect1 = new Rect();
            activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(outRect1);
            result = dm.heightPixels - outRect1.height();
        }
        return result;
    }
}
