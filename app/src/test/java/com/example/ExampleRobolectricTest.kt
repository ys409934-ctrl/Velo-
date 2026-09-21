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

  @Test
  fun `test speech recognizer devanagari hindi commands`() {
    val parser = com.example.data.engine.SpeechToCommandParser

    // Hindi app open
    val ytJson = parser.parseSpeechTextToJson("यूट्यूब खोलो")
    assertEquals("open_app", ytJson.getString("action"))
    assertEquals("youtube", ytJson.getString("app_name"))

    // Hindi camera open
    val camJson = parser.parseSpeechTextToJson("कैमरा चालू करो")
    assertEquals("open_app", camJson.getString("action"))
    assertEquals("camera", camJson.getString("app_name"))

    // Hindi music
    val musicJson = parser.parseSpeechTextToJson("अरिजीत सिंह का गाना बजाओ")
    assertEquals("play_music", musicJson.getString("action"))
    assertTrue(musicJson.getString("query").contains("अरिजीत सिंह"))

    // Hindi WhatsApp message check
    val msgJson = parser.parseSpeechTextToJson("व्हाट्सएप पर कोई मैसेज आया क्या")
    assertEquals("check_messages", msgJson.getString("action"))
    assertEquals("whatsapp", msgJson.getString("platform"))

    // Hindi time check
    val timeJson = parser.parseSpeechTextToJson("अभी समय कितना हुआ है")
    assertEquals("get_time", timeJson.getString("action"))

    // Hindi friendly chat
    val chatJson = parser.parseSpeechTextToJson("नमस्ते आप कौन हो")
    assertEquals("general_chat", chatJson.getString("action"))
    assertTrue(chatJson.getString("reply").contains("Velo"))

    // Owner checks (Yuvraj)
    val owner1 = parser.parseSpeechTextToJson("owner kon ha")
    assertEquals("general_chat", owner1.getString("action"))
    assertTrue(owner1.getString("reply").contains("Yuvraj") || owner1.getString("reply").contains("युवराज"))

    val owner2 = parser.parseSpeechTextToJson("ओनर कौन है तुम्हारा")
    assertEquals("general_chat", owner2.getString("action"))
    assertTrue(owner2.getString("reply").contains("युवराज"))

    val owner3 = parser.parseSpeechTextToJson("malik kaun hai")
    assertEquals("general_chat", owner3.getString("action"))
    assertTrue(owner3.getString("reply").contains("युवराज") || owner3.getString("reply").contains("Yuvraj"))

    val owner4 = parser.parseSpeechTextToJson("yuvraj kaun hai")
    assertEquals("general_chat", owner4.getString("action"))
    assertTrue(owner4.getString("reply").contains("युवराज") || owner4.getString("reply").contains("Yuvraj"))

    val owner5 = parser.parseSpeechTextToJson("who is your owner")
    assertEquals("general_chat", owner5.getString("action"))
    assertTrue(owner5.getString("reply").contains("Yuvraj") || owner5.getString("reply").contains("युवराज"))

    // Call tests
    val call1 = parser.parseSpeechTextToJson("call lagane ka")
    assertEquals("make_call", call1.getString("action"))

    val call2 = parser.parseSpeechTextToJson("call lagao")
    assertEquals("make_call", call2.getString("action"))

    val call3 = parser.parseSpeechTextToJson("call 9876543210")
    assertEquals("make_call", call3.getString("action"))
    assertEquals("9876543210", call3.getString("phone_number"))

    val call4 = parser.parseSpeechTextToJson("papa ko call karo")
    assertEquals("make_call", call4.getString("action"))
    assertEquals("papa", call4.getString("target"))

    val call5 = parser.parseSpeechTextToJson("राहुल को कॉल लगाओ")
    assertEquals("make_call", call5.getString("action"))
    assertEquals("राहुल", call5.getString("target"))
  }
}
