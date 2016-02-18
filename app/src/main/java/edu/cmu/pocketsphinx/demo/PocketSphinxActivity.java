/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package edu.cmu.pocketsphinx.demo;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

public class PocketSphinxActivity extends Activity implements
        RecognitionListener {
		
    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "start";
    private static final String MOVE_ACTION = "direction";

    private static final String UP_ACTION = "up";
    private static final String DOWN_ACTION = "down";
    private static final String RIGHT_ACTION = "right";
    private static final String LEFT_ACTION = "left";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "move to";

    private int posx = 0;
    private int posy = 0;
    private int speed = 20;

    private SpeechRecognizer recognizer;

    private RelativeLayout layout;

    Drawable papyrusDraw;
    Drawable transparent;

    private ImageView papyrus;
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Prepare the data for UI

        setContentView(R.layout.main);
        papyrusDraw =  this.getResources().getDrawable(R.drawable.papyrus);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Preparing the recognizer");
        transparent = this.getResources().getDrawable(android.R.color.holo_red_dark);
        layout =  (RelativeLayout)findViewById(R.id.map);

        papyrus=new ImageView(this);
        papyrus.setImageResource(R.drawable.papyrus);

        layout.addView(papyrus);
        posx = (int)layout.getChildAt(layout.indexOfChild(papyrus)).getX();
        posy = (int)layout.getChildAt(layout.indexOfChild(papyrus)).getY();


        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(PocketSphinxActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    switchSearch(KWS_SEARCH);
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Go");
                }
            }
        }.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recognizer.cancel();
        recognizer.shutdown();
    }
    
    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
    	    return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            ((TextView) findViewById(R.id.caption_text))
                    .setText("Where ?");
            switchSearch(MOVE_ACTION);
        }


    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(UP_ACTION)){
            ((TextView) findViewById(R.id.caption_text)).setText(UP_ACTION);
            System.out.println(UP_ACTION);
            if(posy >0+speed) posy-=speed;

        }
        else if(text.equals(DOWN_ACTION)){
            ((TextView) findViewById(R.id.caption_text)).setText(DOWN_ACTION);
            System.out.println(DOWN_ACTION);
            if(posy < layout.getWidth()-speed) posy+=speed;

        }
        else if(text.equals(RIGHT_ACTION)){
            ((TextView) findViewById(R.id.caption_text)).setText(RIGHT_ACTION);
            System.out.println(RIGHT_ACTION);

            if(posx < layout.getHeight()-speed) posx+=speed;

        }
        else if(text.equals(LEFT_ACTION)){
            ((TextView) findViewById(R.id.caption_text)).setText(LEFT_ACTION);
            System.out.println(LEFT_ACTION);

            if(posx >0+speed) posx-=speed;

        }
        layout.getChildAt(layout.indexOfChild(papyrus)).setX(posx);
        layout.getChildAt(layout.indexOfChild(papyrus)).setY(posy);

        ((TextView) findViewById(R.id.caption_text))
                .setText(" x : " + posx + " y : " + posy);

        layout.refreshDrawableState();

        switchSearch(MOVE_ACTION);
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();
        
        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName,10000);
        else
            recognizer.startListening(searchName,3000);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them
        
        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                
                // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setRawLogDir(assetsDir)
                
                // Threshold to tune for keyphrase to balance between false alarms and misses
                .setKeywordThreshold(1e-45f)
                
                // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)
                
                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
        
        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MOVE_ACTION, menuGrammar);
    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }
}
