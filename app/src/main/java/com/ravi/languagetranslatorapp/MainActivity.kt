package com.ravi.languagetranslatorapp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {
    var et_1: EditText? = null
    var txt: TextView? = null
    var txt_lan_1: TextView? = null
    var txt_lan_2: TextView? = null
    var englishHindiTranslator: Translator? = null
    var hindiEnglishTranslator: Translator? = null
    var clipboard: ClipboardManager? = null
    var clip: ClipData? = null
   // var mdToast: MDToast? = null
    var flag = true
    var dialog: Dialog? = null
    var options_2: TranslatorOptions? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        et_1 = findViewById(R.id.et_1)
        txt = findViewById(R.id.txt)
        txt_lan_1 = findViewById(R.id.txt_lan_1)
        txt_lan_2 = findViewById(R.id.txt_lan_2)
        findViewById<View>(R.id.swap).setOnClickListener(this)
        findViewById<View>(R.id.mic).setOnClickListener(this)
        findViewById<View>(R.id.cp_1).setOnClickListener(this)
        findViewById<View>(R.id.cp_2).setOnClickListener(this)
        clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        dialog = Dialog(this@MainActivity, android.R.style.Theme_Dialog)
        open_dialog()
        et_1?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                // TODO Auto-generated method stub
                if (flag) translate_hin(et_1?.getText().toString()) else translate_eng(
                    et_1?.getText().toString()
                )
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

                // TODO Auto-generated method stub
            }

            override fun afterTextChanged(s: Editable) {

                // TODO Auto-generated method stub
            }
        })

        // Create an English-Hindi translator:
//        val options: TranslatorOptions = TranslatorOptions().builder()
//            .setSourceLanguage(TranslateLanguage.ENGLISH)
//            .setTargetLanguage(TranslateLanguage.HINDI)
//            .build()
        val options: TranslatorOptions = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HINDI)
            .build()

        options_2 = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.HINDI)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        englishHindiTranslator = Translation.getClient(options)
        val conditions: DownloadConditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        englishHindiTranslator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener { // Model downloaded successfully. Okay to start translating.
                download_hin_eng()
            }?.addOnFailureListener { e -> // Model couldn’t be downloaded or other internal error.
                txt?.text = e.message
            }
    }

    fun download_hin_eng() {
        hindiEnglishTranslator = options_2?.let { Translation.getClient(it) }
        hindiEnglishTranslator?.downloadModelIfNeeded()
            ?.addOnSuccessListener(
                OnSuccessListener<Void?> { // Model downloaded successfully. Okay to start translating.
                    // (Set a flag, unhide the translation UI, etc.)
                    //                                findViewById(R.id.btn).setEnabled(true);
                    //                                Toast.makeText(getApplicationContext(),"Second done",Toast.LENGTH_SHORT).show();
                    txt?.text = null
                    dialog!!.dismiss()
                })
            ?.addOnFailureListener { e -> // Model couldn’t be downloaded or other internal error.
                // ...
                txt!!.text = e.message
            }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.mic -> voice()
            R.id.swap -> swap()
            R.id.cp_1 -> try {
                copy(et_1!!.text.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            R.id.cp_2 -> try {
                copy(txt!!.text.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toast(message: String?, type: Int) {
//        mdToast = MDToast.makeText(applicationContext, message, MDToast.LENGTH_SHORT, type)
//        mdToast.show()
    }

    fun voice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        if (flag) intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.ENGLISH
        ) else intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi")
        try {
            startActivityForResult(intent, 200)
        } catch (a: ActivityNotFoundException) {
            toast("Intent Problem", 3)
        }
    }

    fun open_dialog() {
//        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
//        dialog?.setContentView(R.layout.dialog_loading)
//        dialog!!.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
//        dialog!!.setCancelable(false)
//        dialog!!.show()
    }

    fun swap() {
        var a = txt_lan_1!!.text.toString()
        var b = txt_lan_2!!.text.toString()
        a = a + b
        b = a.substring(0, a.length - b.length)
        a = a.substring(b.length)
        txt_lan_1!!.text = a
        txt_lan_2!!.text = b
        flag = if (flag) false else true
        et_1?.setText(null)
        txt?.setText(null)
        toast("Language Changed", 1)
    }

    fun copy(text: String) {
        if (text != "") {
            clip = ClipData.newPlainText("text", text)
           // clipboard!!.setPrimaryClip(clip)
            toast("Text Copied", 1)
        } else {
            toast("Text Copied", 2)
            //            mdToast = MDToast.makeText(getApplicationContext(), "There is no text", MDToast.LENGTH_SHORT, MDToast.TYPE_WARNING);
        }
       // mdToast.show()
    }

    @SuppressLint("StaticFieldLeak")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200) {
            if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!!
                et_1!!.setText(result[0])
                if (flag) translate_hin(
                    et_1!!.text.toString().trim { it <= ' ' }) else translate_eng(
                    et_1!!.text.toString().trim { it <= ' ' })
            }
        }
    }

    fun translate_hin(text: String?) {
        if (text != null) {
            englishHindiTranslator?.translate(text)
                ?.addOnSuccessListener { p0 -> txt!!.text = p0 }
                ?.addOnFailureListener { e -> // Error.
                    // ...
                    txt!!.text = e.message
                }
        }
    }

    fun translate_eng(text: String?) {
        if (text != null) {
            hindiEnglishTranslator?.translate(text)
                ?.addOnSuccessListener { p0 -> txt!!.text = p0 }
                ?.addOnFailureListener { e -> // Error.
                    // ...
                    txt!!.text = e.message
                }
        }
    }
}