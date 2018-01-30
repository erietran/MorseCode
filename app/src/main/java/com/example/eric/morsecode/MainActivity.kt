package com.example.eric.morsecode

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mtextView.movementMethod = ScrollingMovementMethod()
        testButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString());
            hideKeyboard();
        }
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
    fun Activity.hideKeyboard() {
        hideKeyboard(if(currentFocus == null) View( this) else currentFocus)
    }

    fun Context.hideKeyboard(view: View){
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}





