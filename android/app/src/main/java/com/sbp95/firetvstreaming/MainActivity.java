package com.sbp95.firetvstreaming;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.MediaController;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Minimal native Activity for Fire OS / Android TV (API 23+). */
public final class MainActivity extends Activity {
  private static final String PREFS = "fire_tv_streaming";
  private String serverUrl = "";
  private VideoView videoView;

  @Override public void onCreate(Bundle state) { super.onCreate(state); showSetup(); }

  private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }
  private TextView text(String value, int size) {
    TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(Color.WHITE); view.setPadding(0, dp(8), 0, dp(8)); return view;
  }
  private LinearLayout page() {
    LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setGravity(Gravity.CENTER_VERTICAL); layout.setPadding(dp(48), dp(28), dp(48), dp(28)); layout.setBackgroundColor(Color.rgb(16,16,20)); return layout;
  }
  private void showSetup() {
    LinearLayout layout = page(); layout.addView(text("Fire TV Streaming", 30)); layout.addView(text("Adresse des Linux- oder PC-Servers", 18));
    EditText address = new EditText(this); address.setSingleLine(true); address.setTextColor(Color.WHITE); address.setHintTextColor(Color.LTGRAY); address.setHint("http://192.168.178.20:8080"); address.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString("server", "")); layout.addView(address, new LinearLayout.LayoutParams(-1, -2));
    Button load = new Button(this); load.setText("Mediathek laden"); layout.addView(load);
    load.setOnClickListener(v -> { String value = address.getText().toString().trim(); if (value.isEmpty()) { Toast.makeText(this, "Bitte Server-Adresse eingeben", Toast.LENGTH_LONG).show(); return; } serverUrl = value.replaceAll("/+$", ""); getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("server", serverUrl).apply(); fetchLibrary(); });
    setContentView(layout);
  }
  private void fetchLibrary() {
    new Thread(() -> {
      try {
        HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl + "/api/library").openConnection(); connection.setConnectTimeout(7000); connection.setReadTimeout(7000);
        if (connection.getResponseCode() != 200) throw new IllegalStateException("HTTP " + connection.getResponseCode());
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())); StringBuilder body = new StringBuilder(); String line; while ((line = reader.readLine()) != null) body.append(line); reader.close();
        JSONArray movies = new JSONArray(body.toString()); runOnUiThread(() -> showLibrary(movies));
      } catch (Exception error) { runOnUiThread(() -> Toast.makeText(this, "Server nicht erreichbar. Prüfe WLAN und Adresse.", Toast.LENGTH_LONG).show()); }
    }).start();
  }
  private void showLibrary(JSONArray movies) {
    LinearLayout list = page(); list.addView(text("Meine Mediathek", 30));
    if (movies.length() == 0) list.addView(text("Noch keine Videos hochgeladen.", 18));
    for (int index = 0; index < movies.length(); index++) {
      try { JSONObject movie = movies.getJSONObject(index); Button button = new Button(this); button.setText(movie.optString("title", "Unbenannt")); final String id = movie.getString("id"); button.setOnClickListener(v -> play(id)); list.addView(button); } catch (Exception ignored) { }
    }
    ScrollView scroll = new ScrollView(this); scroll.addView(list); setContentView(scroll);
  }
  private void play(String id) {
    videoView = new VideoView(this); videoView.setBackgroundColor(Color.BLACK); MediaController controls = new MediaController(this); controls.setAnchorView(videoView); videoView.setMediaController(controls); videoView.setVideoPath(serverUrl + "/api/movies/" + id + "/stream"); videoView.setOnErrorListener((mp, what, extra) -> { Toast.makeText(this, "Video kann nicht abgespielt werden. MP4 mit H.264/AAC verwenden.", Toast.LENGTH_LONG).show(); return true; }); setContentView(videoView); videoView.requestFocus(); videoView.start();
  }
  @Override public void onBackPressed() { if (videoView != null) { videoView.stopPlayback(); videoView = null; fetchLibrary(); } else { showSetup(); } }
  @Override protected void onStop() { if (videoView != null) videoView.pause(); super.onStop(); }
}
