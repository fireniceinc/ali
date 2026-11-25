package com.fireniceinc.ali;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(32, 32, 32, 32);
        box.setBackgroundColor(Color.rgb(7, 11, 16));

        TextView title = new TextView(this);
        title.setText("ALI");
        title.setTextSize(42);
        title.setTextColor(Color.rgb(255, 107, 53));
        title.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);
        status.setText(
            "Architecture B \u2022 Android bootstrap\n\n" +
            "GitHub Actions build is operational."
        );
        status.setTextSize(18);
        status.setTextColor(Color.WHITE);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 24, 0, 0);

        box.addView(title);
        box.addView(status);
        setContentView(box);
    }
}
