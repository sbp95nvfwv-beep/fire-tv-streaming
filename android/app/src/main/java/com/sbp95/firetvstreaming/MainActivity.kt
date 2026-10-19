package com.sbp95.firetvstreaming

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
  private val preferences by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
  private var player: ExoPlayer? = null
  override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showServerSetup() }
  private fun showServerSetup() {
    val address = EditText(this).apply { hint = "Server-Adresse, z. B. http://192.168.1.10:8080"; setText(preferences.getString("server", "")) }
    val button = Button(this).apply { text = "Mediathek laden"; setOnClickListener { preferences.edit().putString("server", address.text.toString().trimEnd('/')).apply(); loadLibrary(address.text.toString().trimEnd('/')) } }
    val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48,48,48,48); addView(address); addView(button) }
    setContentView(layout)
  }
  private fun loadLibrary(server: String) = thread {
    try { val connection = URL("$server/api/library").openConnection() as HttpURLConnection; val items = JSONArray(connection.inputStream.bufferedReader().readText()); runOnUiThread { showLibrary(server, items) } } catch (_: Exception) { runOnUiThread { Toast.makeText(this, "Server nicht erreichbar", Toast.LENGTH_LONG).show() } }
  }
  private fun showLibrary(server: String, items: JSONArray) {
    val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48,32,48,32) }; list.addView(TextView(this).apply { text = "Meine Mediathek"; textSize = 28f })
    for (i in 0 until items.length()) { val movie = items.getJSONObject(i); list.addView(Button(this).apply { text = movie.getString("title"); setOnClickListener { play("$server/api/movies/${movie.getString("id")}/stream") } }) }; setContentView(ScrollView(this).apply { addView(list) })
  }
  private fun play(url: String) { player?.release(); player = ExoPlayer.Builder(this).build(); setContentView(PlayerView(this).apply { this.player = this@MainActivity.player; layoutParams = ViewGroup.LayoutParams(-1,-1) }); player?.setMediaItem(MediaItem.fromUri(url)); player?.prepare(); player?.play() }
  override fun onStop() { player?.pause(); super.onStop() }
  override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
