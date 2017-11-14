package com.softsol.e7kily;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.widget.Toast;

import com.softsol.e7kily.helpers.LevenshteinDistance;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextToSpeech tts;

    private MediaPlayer mediaPlayer;
    private int playbackPosition = 0;

    private String searchText = null;

    private PDFTextStripper pdfStripper;
    private PDDocument document;
    private int i = 0;

    private File root;
    private AssetManager assetManager;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setup();
    }

    @Override
    public void onPause() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        killMediaPlayer();
        if (document != null) try {
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                recognition(inSpeech);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("PER", "Permission: " + permissions[0] + "was " + grantResults[0]);
            //resume tasks needing this permission
            readPDF();
        }
    }

    /**
     * Initializes variables used for convenience
     */
    private void setup() {
        findViewById(R.id.microphoneButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listen();
            }
        });
        // Enable Android-style asset loading (highly recommended)
        PDFBoxResourceLoader.init(getApplicationContext());
        // Find the root of the external storage.
        root = android.os.Environment.getExternalStorageDirectory();
        assetManager = getAssets();
        tv = (TextView) findViewById(R.id.statusTextView);
    }

    private void listen() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");

        try {
            startActivityForResult(i, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(MainActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }

    private void recognition(String text) {
        Log.e("Speech", "" + text);

        if (LevenshteinDistance.GetDiffernceRate(text, "resume") <= .3) {
            restartAudio();
        } else if (LevenshteinDistance.GetDiffernceRate(text, "pause") <= .3) {
            pauseAudio();
        } else if (LevenshteinDistance.GetDiffernceRate(text, "stop") <= .3) {
            tts.stop();
            stopAudio();
        } else {
            searchText = text;
            if (isStoragePermissionGranted()) {
                readPDF();
            }
        }
    }

    private void readPDF() {
        ArrayList<File> PDFs = new ArrayList<>();
        walkdir(Environment.getExternalStorageDirectory(), PDFs);
        if (PDFs.size() > 0) {
            stripText(PDFs.get(MakeMatching(PDFs, searchText)));
        } else {
            Toast.makeText(this, "No PDF files Found", Toast.LENGTH_SHORT).show();
        }
    }

    private int MakeMatching(ArrayList<File> files, String target) {
        int bestFileIndex = -1;
        float minDis = 1;

        for (int i = 0; i < files.size(); i++) {
            float dis = LevenshteinDistance.GetDiffernceRate(target, files.get(i).getName().substring(0, files.get(i).getName().length() - 4));
            if (dis < minDis) {
                minDis = dis;
                bestFileIndex = i;
            }
        }

        return bestFileIndex;
    }

    private void walkdir(File dir, ArrayList<File> PDFs) {
        String pdfPattern = ".pdf";

        File listFile[] = dir.listFiles();

        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {

                if (listFile[i].isDirectory()) {
                    walkdir(listFile[i], PDFs);
                } else {
                    if (listFile[i].getName().endsWith(pdfPattern)) {
                        //Do what ever u want
                        PDFs.add(listFile[i]);
                    }
                }
            }
        }
    }

    private void speak(final String text) {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(MainActivity.this, "This Language is not supported", Toast.LENGTH_SHORT).show();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "1");
                        } else {
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Initilization Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {

            }

            @Override
            public void onDone(String utteranceId) {
                try {
                    handleSpeech(++i, pdfStripper, document);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    /**
     * Strips the text from a PDF and displays the text on screen
     */
    private void stripText(File file) {
        try {
            document = PDDocument.load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (!document.isEncrypted()) {
                pdfStripper = new PDFTextStripper();

                i = 0;
                handleSpeech(++i, pdfStripper, document);
            } else {
                Toast.makeText(this, "PDF file is Encrypted", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
//                if (document != null) document.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSpeech(int i, PDFTextStripper pdfStripper, PDDocument document) throws IOException {
        String parsedText = null;

        if (i <= document.getNumberOfPages()) {
            pdfStripper.setStartPage(i);
            pdfStripper.setEndPage(i);

            parsedText = pdfStripper.getText(document);

            final String finalParsedText = parsedText;
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv.setText(finalParsedText);
                }
            });

            Bidi bidi = new Bidi(parsedText, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);

            if (bidi.baseIsLeftToRight()) {
                speak(parsedText);
            } else {
                try {
                    String fileUrl = "http://127.0.0.1:5000/api/v1.0/tts/" + parsedText;
                    playAudio(fileUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("PER", "Permission is granted");
                return true;
            } else {

                Log.v("PER", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("PER", "Permission is granted");
            return true;
        }
    }

    public String getEncodedURL(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        urlStr = uri.toASCIIString();
        return urlStr;
    }

    private void playAudio(String url) throws Exception {
        if (!URLUtil.isNetworkUrl(url)) {
            throw new RuntimeException("Invalid Url");
        }

        killMediaPlayer();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(getEncodedURL(url));
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            playbackPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    private void restartAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(playbackPosition);
            mediaPlayer.start();
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playbackPosition = 0;
        }
    }

    private void killMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
