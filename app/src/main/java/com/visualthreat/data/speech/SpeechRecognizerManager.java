
package com.visualthreat.data.speech;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

/**
 * Created by USER on 1/2/2017.
 */
public class SpeechRecognizerManager {

    private static final String TAG             = "SpeechRecognizerManager";
    private static final String KWS_SEARCH      = "wakeup";
    private static final String KEYPHRASE       = "command begin";
    private static final String DIGITS_SEARCH   = "digits search";
    public static final int START               = 0x1000;
    public static final int STOP                = 0x1001;
    public static final int TURN_LEFT           = 0x1002;
    public static final int TURN_RIGHT          = 0x1003;
    public static final int STRAIGHT            = 0x1004;
    public static final int BACKUP              = 0x1005;
    public static final int TRAFFIC             = 0x1006;
    public static final int U_TURN              = 0x1007;
    public static final int LOCAL               = 0x1008;
    public static final int FREEWAY             = 0x1009;
    public static final int RED_LIGHT           = 0x1010;
    public static final int GREEN_LIGHT         = 0x1011;
    private static final int MAX_STOP           = 15;
    private edu.cmu.pocketsphinx.SpeechRecognizer mPocketSphinxRecognizer;
    private static String Search;
    private Context mContext;
    private OnResultListener mOnResultListener;
    private boolean flags = false;
    private int count = 0;

    //if countStop = MAX_STOP stop record
    private int countStop = 0;

    public SpeechRecognizerManager(Context context) {
        this.mContext = context;
        initPockerSphinx();
    }

    public void Wakeup() {
        mPocketSphinxRecognizer.cancel();
        restartSearch(DIGITS_SEARCH);
        mOnResultListener.OnResult(START);
        countStop = 0;
    }

    public void Stop() {
        mOnResultListener.OnResult(STOP);
        restartSearch(KWS_SEARCH);
    }
    /**
     * Destroy core sphinx
     */
    public void OnDestroy() {
        destroy();
    }

    /**
     * init core sphinx
     */
    private void initPockerSphinx() {

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(mContext);
                    File assetDir = assets.syncAssets();
                    SpeechRecognizerSetup speechRecognizerSetup = defaultSetup();
                    speechRecognizerSetup.setAcousticModel(new File(assetDir, "en-us-ptm"));
                    speechRecognizerSetup.setDictionary(new File(assetDir, "cmudict-en-us.dict"));
                    speechRecognizerSetup.setKeywordThreshold(1e-45f);
                    //Creates a new SpeechRecognizer object based on previous set up.
                    mPocketSphinxRecognizer = speechRecognizerSetup.getRecognizer();
                    mPocketSphinxRecognizer.addListener(new PocketSphinxRecognitionListener());

                    // Create keyword-activation search.
                    mPocketSphinxRecognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
                    File digitsGrammar = new File(assetDir, "grammar.gram");
                    mPocketSphinxRecognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    Toast.makeText(mContext, "Failed to init mPocketSphinxRecognizer ", Toast.LENGTH_SHORT).show();
                } else {
                    restartSearch(KWS_SEARCH);
                }
            }
        }.execute();

    }

    public void destroy() {
        if (mPocketSphinxRecognizer != null) {
            mPocketSphinxRecognizer.cancel();
            mPocketSphinxRecognizer.shutdown();
            mPocketSphinxRecognizer = null;
        }
    }
    /**
     * start session search text
     * @param searchName
     */
    private void restartSearch(String searchName) {
        Search = searchName;
        mPocketSphinxRecognizer.stop();
        mPocketSphinxRecognizer.startListening(searchName);
    }


    /**
     * Listen the result of processing void
     */
    protected class PocketSphinxRecognitionListener implements edu.cmu.pocketsphinx.RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            if (hypothesis == null) {
                Log.d(TAG, "null");
                return;
            }
            String text = hypothesis.getHypstr();
            if (text.equals(KEYPHRASE)) {
                mPocketSphinxRecognizer.cancel();
                restartSearch(DIGITS_SEARCH);
                mOnResultListener.OnResult(START);
                countStop = 0;
                //Toast.makeText(mContext, "You said: " + text, Toast.LENGTH_SHORT).show();
            }
            if (text != null) {
                Log.d(TAG, "text: " + text);
                if (text.equals("command turn left")) {
                    //Toast.makeText(mContext, "You said: command turn left", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(TURN_LEFT);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command turn right")) {
                    //Toast.makeText(mContext, "You said: command turn right", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(TURN_RIGHT);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command stop logging")) {
                    //Toast.makeText(mContext, "You said: command stop logging", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(STOP);
                    restartSearch(KWS_SEARCH);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command back up")) {
                    //Toast.makeText(mContext, "You said: command back up", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(BACKUP);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command straight")) {
                    //Toast.makeText(mContext, "You said: command straight", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(STRAIGHT);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command traffic")) {
                    //Toast.makeText(mContext, "You said: command traffic", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(TRAFFIC);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command u turn")) {
                    //Toast.makeText(mContext, "You said: command u turn", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(U_TURN);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command local")) {
                    //Toast.makeText(mContext, "You said: command local", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(LOCAL);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command freeway")){
                    //Toast.makeText(mContext, "You said: command freeway", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(FREEWAY);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command red light")) {
                    //Toast.makeText(mContext, "You said: command red light", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(RED_LIGHT);
                    flags = true;
                    countStop = 0;
                } else if (text.equals("command green light") ){
                    //Toast.makeText(mContext, "You said: command green light", Toast.LENGTH_SHORT).show();
                    mOnResultListener.OnResult(GREEN_LIGHT);
                    flags = true;
                    countStop = 0;
                }
                else {
                    count ++;
                    Log.d(TAG,"count: " + count);
                    if(count > 2) {
                        flags = true;
                        countStop ++;
                        if(countStop >= MAX_STOP){
                            //Stop();
                        }
                    }
                }
                if(flags) {
                    mPocketSphinxRecognizer.cancel();
                    mPocketSphinxRecognizer.startListening(Search);
                    flags = false;
                    count = 0;
                }
            }
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
            //Log.d(TAG,"text result: " + hypothesis.getHypstr());
        }

        @Override
        public void onEndOfSpeech() {

        }

        public void onError(Exception error) {
        }

        @Override
        public void onTimeout() {
            Log.d(TAG,"text time out");
        }
    }

    public void setOnResultListner(OnResultListener onResultListener) {
        mOnResultListener = onResultListener;
    }

    /**
     * Interface the result to return MainActivity
     */
    public interface OnResultListener {
        public void OnResult(int result);
    }
}
