package com.example.receitas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imgLogo = findViewById(R.id.imgLogoSplash);
        TextView tvNome = findViewById(R.id.tvNomeAppSplash);
        TextView tvSlogan = findViewById(R.id.tvSloganSplash);

        // 1. Esconder tudo e encolher o logo ligeiramente no início
        imgLogo.setAlpha(0f);
        imgLogo.setScaleX(0.8f);
        imgLogo.setScaleY(0.8f);
        tvNome.setAlpha(0f);
        tvSlogan.setAlpha(0f);

        // 2. Animar o Logótipo (Fade in e crescer)
        imgLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .start();

        // 3. Animar o Título e o Slogan logo a seguir
        tvNome.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(400)
                .start();

        tvSlogan.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(500)
                .withEndAction(() -> {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }, 1200);
                })
                .start();
    }
}
