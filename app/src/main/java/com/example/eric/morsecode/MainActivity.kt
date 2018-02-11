package com.example.eric.morsecode

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils.lastIndexOf
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.lang.Math.round
import java.util.*
import kotlin.concurrent.timerTask

val SAMPLE_RATE = 44100;


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mtextView.movementMethod = ScrollingMovementMethod()
        testButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString());
            hideKeyboard();
        }
        buildDictsWithJSON(jsonObj = loadMorseJSON())
        showButton.setOnClickListener{
            showCodes()
            hideKeyboard()
        }
        transButton.setOnClickListener{
            appendTextAndScroll(translate((inputText.text.toString()).toLowerCase()))
            hideKeyboard()
        }
        playImage.setOnClickListener{
            playString(toMorse((inputText.text.toString()).toLowerCase()), 0)
            hideKeyboard()
        }


    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    var letToCodeDict: HashMap<String, String> = HashMap();
    var codeToLetDict: HashMap<String, String> = HashMap();

    fun buildDictsWithJSON(jsonObj : JSONObject){
        for(k in jsonObj.keys()){
            val code = jsonObj.getString(k)
            letToCodeDict.put(k, code)
            codeToLetDict.put(code, k)
        }
    }

    fun showCodes(){
        appendTextAndScroll("HERE ARE THE CODES")
        for(k in letToCodeDict.keys.sorted())
            appendTextAndScroll("$k: ${letToCodeDict[k]}")
    }

    private fun appendTextAndScroll(text: String) {
        if(mtextView!= null) {
            mtextView.append(text + "\n")
            val layout = mtextView.getLayout()
            if (layout != null){
                val scrollDelta = (layout!!.getLineBottom( mtextView.getLineCount() - 1)
                        - mtextView.getScrollY() - mtextView.getHeight());
                if (scrollDelta > 0)
                    mtextView.scrollBy( 0, scrollDelta);

            }
        }
    }
    fun loadMorseJSON() : JSONObject {
        val filePath = "morse.json"
        val jsonStr = application.assets.open(filePath).bufferedReader().use{
            it.readText()
        }
        val jsonObj = JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1))
        return jsonObj;
    }
    fun Activity.hideKeyboard() {
        hideKeyboard(if(currentFocus == null) View( this) else currentFocus)
    }

    fun Context.hideKeyboard(view: View){
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun translate(text: String) : String{
        var newString = ""
        var morseBool = false
        val morseChecker = text.split(' ', limit = 0)
        for(k : String? in morseChecker) {
            if (codeToLetDict.containsKey(k))
                if (k != "/")
                    morseBool = true
        }
        if(morseBool){
            for(k : String? in morseChecker) {
                if (codeToLetDict.containsKey(k))
                    newString = newString.plus(codeToLetDict.get(k))
                else
                    newString = newString.plus("?")
            }
        }
        else
            for(i in text.indices){
                if (text[i] == ' ')
                    newString = newString.plus("/ ")
                else if(letToCodeDict.containsKey(text[i].toString())) {
                    newString = newString.plus(letToCodeDict.get(text[i].toString()) + " ")
                }
                else
                    newString = newString.plus("? ")
            }

    return newString
    }
    fun toMorse(text: String) : String{
        var newString = ""
        var morseBool = false
        val morseChecker = text.split(' ', limit = 0)
        for(k : String? in morseChecker) {
            if (codeToLetDict.containsKey(k))
                if (k != "/")
                    morseBool = true
        }
        if(morseBool){
           return text
        }
        else
            for(i in text.indices){
                if (text[i] == ' ')
                    newString = newString.plus("/ ")
                else if(letToCodeDict.containsKey(text[i].toString())) {
                    newString = newString.plus(letToCodeDict.get(text[i].toString()) + " ")
                }
                else
                    newString = newString.plus("? ")
            }

        return newString
    }

    fun playString(s:String, i: Int = 0) : Unit{
        if(i > s.length - 1)
            return;

        var mDelay: Long = 0;

        var thenFun: () -> Unit = { ->
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                playString(s, i+1)
            })
        }
        var c = s[i];
        Log.d("Log", "Processing pos: " + i + "  char: [" + c +"]")
        if(c =='.')
            playDot( thenFun)
        else if(c == '-')
            playDash(thenFun)
        else if(c == '/')
            pause( 6*dotLength, thenFun)
        else if(c == ' ')
            pause( 2*dotLength, thenFun)
    }

    val dotLength:Int = 50;
    val dashLength:Int = dotLength * 3
    val dotSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dotLength)
    val dashSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dotLength)

    fun playDash(onDone : () -> Unit = { }){
        Log.d( "DEBUG", "playDash")
        playSoundBuffer(dashSoundBuffer, { -> pause(dashLength, onDone)})
    }

    fun playDot(onDone : () -> Unit = { }){
        Log.d( "DEBUG", "playDot")
        playSoundBuffer(dotSoundBuffer, { -> pause(dotLength, onDone)})
    }

    fun pause(durationMSec: Int, onDone : () -> Unit = { } ){
        Log.d("DEBUG", "pause: " + durationMSec)
        Timer().schedule( timerTask{
            onDone()
        }, durationMSec.toLong())
    }

    private fun genSineWaveSoundBuffer( frequency: Double, durationMSec: Int ) : ShortArray {
        val duration: Int = round((durationMSec / 1000.0) * SAMPLE_RATE).toInt()
        var mSound : Double
        val mBuffer = ShortArray(duration)
        for(i in 0 until duration) {
          mSound = Math.sin(2.0 * Math.PI * i.toDouble() / (SAMPLE_RATE / frequency));
            mBuffer[i] = (mSound * java.lang.Short.MAX_VALUE).toShort()
        }

        return mBuffer
    }
    private fun playSoundBuffer(mBuffer:ShortArray, onDone : () -> Unit = { } ) {
        var minBufferSize = SAMPLE_RATE/10;
        if(minBufferSize < mBuffer.size) {
            minBufferSize = minBufferSize + minBufferSize *
                    (Math.round( mBuffer.size.toFloat()) / minBufferSize.toFloat() ).toInt();
        }
        val nBuffer = ShortArray(minBufferSize);
        for (i in nBuffer.indices) {
            if(i < mBuffer.size) nBuffer[i] = mBuffer[i];
            else nBuffer[i] = 0;
        }
        val mAudioTrack = AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM)
        mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume())
        mAudioTrack.setNotificationMarkerPosition(mBuffer.size)
        mAudioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onPeriodicNotification(track: AudioTrack) { }
            override fun onMarkerReached(track: AudioTrack) {
                Log.d( "Log", "Audio track end of file reached...")
                mAudioTrack.stop(); mAudioTrack.release(); onDone();
            }
        })
        mAudioTrack.play(); mAudioTrack.write(nBuffer, 0, minBufferSize)
    }
}





