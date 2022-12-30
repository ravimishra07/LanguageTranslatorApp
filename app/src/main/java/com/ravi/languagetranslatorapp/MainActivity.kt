package com.ravi.languagetranslatorapp

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH
import com.google.mlkit.nl.translate.TranslateLanguage.HINDI
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.ravi.languagetranslatorapp.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var engToHiTranslatorOption: TranslatorOptions
    private lateinit var hiToEngTranslatorOption: TranslatorOptions
    private lateinit var engToHiTranslator: Translator
    private lateinit var hiToEngTranslator: Translator

    private var mInterstitialAd: InterstitialAd? = null
    private val testAddId = "ca-app-pub-3940256099942544/1033173712"
    val prodAddId = "ca-app-pub-4749277994804943/3818428720"
    var isTranslateToHindi = true
   lateinit var clipboard: ClipboardManager
    lateinit var clipData: ClipData


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        setTextWatchers()
        setTranslator()
        initializeInterstitialAdds()
        initializeBannerAdd()
        setListeners()
    }
    fun speechToTextTapped() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        if (isTranslateToHindi) intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.ENGLISH
        ) else intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi")
        try {
            startActivityForResult(intent, 200)
        } catch (a: ActivityNotFoundException) {
            binding.root.toast("Could not convert speech")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200) {
            if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!!
                binding.etFirstLang.setText(result[0])
                if (isTranslateToHindi){
                    englishToHindi(binding.etFirstLang.text.toString().trim { it <= ' ' })
                }else {
                    hindiToEnglish(binding.etFirstLang.text.toString().trim { it <= ' ' })
                }
            }
        }
    }
    private fun hideSoftKeyboard() {
        val inputMethodManager: InputMethodManager = getSystemService(
            INPUT_METHOD_SERVICE
        ) as InputMethodManager
        if (inputMethodManager.isAcceptingText) {
            inputMethodManager.hideSoftInputFromWindow(
                currentFocus!!.windowToken,
                0
            )
        }
    }
    private fun switchLanguage(){
        isTranslateToHindi = !isTranslateToHindi
        if(isTranslateToHindi){
            binding.tvFirst.text = getText(R.string.english)
            binding.tvSecond.text = getText(R.string.hindi)
            binding.tvFirstSideLabel.text =getText(R.string.english)
            binding.tvSecondSideLabel.text =getText(R.string.hindi)
            binding.etFirstLang.hint =getText(R.string.write_something_here)
        }else{
            binding.tvFirst.text = getText(R.string.hindi)
            binding.tvSecond.text = getText(R.string.english)
            binding.tvFirstSideLabel.text =getText(R.string.hindi)
            binding.tvSecondSideLabel.text =getText(R.string.english)
            binding.etFirstLang.hint =getText(R.string.write_something_here_hindi)
        }
    }
    private fun copyToClipBoard(text: String) {
        if (text != "") {
            hideSoftKeyboard()
            showInterstitialAdd()
            clipData = ClipData.newPlainText("text", text)
            clipboard.setPrimaryClip(clipData)
            binding.root.toast("Text Copied")
        }
    }
    private fun rotateSwitchIcon(){
        val rotate = RotateAnimation(
            0F,
            180F,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotate.duration = 300
        rotate.interpolator = LinearInterpolator()
        binding.ivLangSwitch.startAnimation(rotate)
    }

    private fun setTranslator() {

        // Create an English-Hindi translator:
        engToHiTranslatorOption = TranslatorOptions.Builder()
            .setSourceLanguage(ENGLISH)
            .setTargetLanguage(HINDI)
            .build()

        hiToEngTranslatorOption = TranslatorOptions.Builder()
            .setSourceLanguage(HINDI)
            .setTargetLanguage(ENGLISH)
            .build()

        engToHiTranslator = Translation.getClient(engToHiTranslatorOption)
        hiToEngTranslator = Translation.getClient(hiToEngTranslatorOption)
        downloadTranslatorModels()
    }
    private fun setListeners(){
        binding.ivLangSwitch.setOnClickListener {
            switchLanguage()
            rotateSwitchIcon()
        }
        binding.ivCopyFirst.setOnClickListener {
           copyToClipBoard(binding.etFirstLang.text.toString())
        }
        binding.ivCopySecond.setOnClickListener {
            copyToClipBoard(binding.tvSecondLang.text.toString())
        }
        binding.ivMic.setOnClickListener {
            speechToTextTapped()
        }
    }

    private fun downloadTranslatorModels() {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        engToHiTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener { // Model downloaded successfully. Okay to start translating.
                downloadTranslatorLanguage()
            }
            .addOnFailureListener { e -> // Model couldn’t be downloaded or other internal error.
                binding.tvSecondLang.text = e.message
            }
    }

    private fun downloadTranslatorLanguage() {
        engToHiTranslator = Translation.getClient(engToHiTranslatorOption)
        engToHiTranslator.downloadModelIfNeeded()
            .addOnSuccessListener { // Model downloaded successfully. Okay to start translating.
                //  txt.setText(null)
                binding.tvSecondLang.text = null
            }
            .addOnFailureListener { e -> // Model couldn’t be downloaded or other internal error.
                // ...
                // txt.setText(e.message)
            }
    }

    private fun initializeInterstitialAdds() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            testAddId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError?.toString())
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    setAddListener()
                }
            })

    }

    private fun showInterstitialAdd(){
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this@MainActivity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
        }
    }
    private fun initializeBannerAdd(){
        MobileAds.initialize(this) {}

        val adRequest1 = AdRequest.Builder().build()
        val adRequest2 = AdRequest.Builder().build()
        binding.adViewMed.loadAd(adRequest1)
        binding.adViewBottom.loadAd(adRequest2)

        binding.adViewMed.adListener = object: AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        }

        binding.adViewBottom.adListener = object: AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        }

    }

    private fun setTextWatchers() {
        binding.etFirstLang.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (isTranslateToHindi) {
                    englishToHindi(binding.etFirstLang.text.toString())
                } else {
                    hindiToEnglish(binding.etFirstLang.text.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {
            }
        })
    }

    fun setAddListener() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                mInterstitialAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }
    }
    fun englishToHindi(text: String?) {
        if (text != null) {
            engToHiTranslator.translate(text)
                .addOnSuccessListener { translatedText -> // Translation successful.
                    binding.tvSecondLang.text = translatedText
                }
                .addOnFailureListener { e -> // Error.
                    binding.tvSecondLang.text = e.message
                }
        }
    }

    fun hindiToEnglish(text: String?) {
        if (text != null) {
            hiToEngTranslator.translate(text)
                .addOnSuccessListener { translatedText ->
                    binding.tvSecondLang.text = translatedText
                }
                .addOnFailureListener { e -> // Error.
                    // ...
                    binding.tvSecondLang.text = e.message
                }
        }
    }
    companion object {
        var TAG = "yayy"//this::class.java.simpleName.toString()
    }
}
fun View.toast(text: String){
    Toast.makeText(this.context,text, Toast.LENGTH_LONG).show()
}
