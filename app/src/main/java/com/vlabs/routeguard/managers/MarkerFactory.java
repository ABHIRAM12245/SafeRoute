package com.vlabs.routeguard.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.ReportType;

public class MarkerFactory {

    public static Drawable getMarkerIcon(Context context, ReportType type) {
        int iconResId;
        int color;

        switch (type) {
            case POLICE_CHECKING:
                iconResId = R.drawable.ic_police;
                color = Color.parseColor("#1976D2"); // Blue
                break;
            case ACCIDENT:
                iconResId = R.drawable.ic_accident;
                color = Color.parseColor("#D32F2F"); // Red
                break;
            case HEAVY_TRAFFIC:
                iconResId = R.drawable.ic_traffic;
                color = Color.parseColor("#F57C00"); // Orange
                break;
            case ROAD_CONSTRUCTION:
                iconResId = R.drawable.ic_construction;
                color = Color.parseColor("#FBC02D"); // Yellow
                break;
            case ANIMAL_ON_ROAD:
                iconResId = R.drawable.ic_animal;
                color = Color.parseColor("#795548"); // Brown
                break;
            case WATER_LOGGING:
                iconResId = R.drawable.ic_water_logging;
                color = Color.parseColor("#0288D1"); // Light Blue
                break;
            case OTHER:
            default:
                iconResId = R.drawable.ic_other;
                color = Color.parseColor("#757575"); // Grey
                break;
        }

        Drawable vectorDrawable = ContextCompat.getDrawable(context, iconResId);
        if (vectorDrawable == null) return null;

        // Consistent marker size
        int size = (int) (36 * context.getResources().getDisplayMetrics().density);

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Draw background circle
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Draw icon in the center
        int padding = (int) (8 * context.getResources().getDisplayMetrics().density);
        vectorDrawable.setBounds(padding, padding, size - padding, size - padding);
        vectorDrawable.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        vectorDrawable.draw(canvas);

        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
