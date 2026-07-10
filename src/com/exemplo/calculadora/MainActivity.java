package com.exemplo.calculadora;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);

        // 🌈 Gradiente azul → vermelho
        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        gradient.setColors(new int[]{
                Color.parseColor("#1E3A8A"),  // azul escuro
                Color.parseColor("#DC2626")   // vermelho
        });
        rootLayout.setBackground(gradient);

        CalculadoraView calculadora = new CalculadoraView(this);
        rootLayout.addView(calculadora);

        setContentView(rootLayout);
    }
}