package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.VeloLocalParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Velo", appName)
  }

  @Test
  fun `test open youtube command returns open_app json`() {
    val json = VeloLocalParser.parseToStructuredJson("open youtube")
    assertEquals("open_app", json.getString("action"))
    assertEquals("youtube", json.getString("app_name"))
  }

  @Test
  fun `test hindi open app command`() {
    val json = VeloLocalParser.parseToStructuredJson("insta open karo")
    assertEquals("open_app", json.getString("action"))
    assertEquals("instagram", json.getString("app_name"))
  }

  @Test
  fun `test play music command`() {
    val json = VeloLocalParser.parseToStructuredJson("play song")
    assertEquals("play_music", json.getString("action"))
  }

  @Test
  fun `test hindi play music command`() {
    val json = VeloLocalParser.parseToStructuredJson("gaana bajao")
    assertEquals("play_music", json.getString("action"))
  }

  @Test
  fun `test check messages command`() {
    val json = VeloLocalParser.parseToStructuredJson("whatsapp check karo")
    assertEquals("check_messages", json.getString("action"))
    assertEquals("whatsapp", json.getString("platform"))
  }

  @Test
  fun `test check time command`() {
    val json = VeloLocalParser.parseToStructuredJson("क्या समय हुआ है")
    assertEquals("get_time", json.getString("action"))
  }

  @Test
  fun `test general chat command`() {
    val json = VeloLocalParser.parseToStructuredJson("kaise ho")
    assertEquals("general_chat", json.getString("action"))
    assertTrue(json.getString("reply").isNotEmpty())
  }
}
