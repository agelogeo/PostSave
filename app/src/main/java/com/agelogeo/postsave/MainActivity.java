package com.agelogeo.postsave;

import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
  TextView urlText;
  Button downloadButton;
  ImageView logoImageView;
  String link;

  public class ImageDownloader extends AsyncTask<String,Void, Bitmap> {

    @Override
    protected Bitmap doInBackground(String... urls) {
      try{
        URL url = new URL(urls[0]);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.connect();

        InputStream inputStream = httpURLConnection.getInputStream();
        Bitmap myBitmap = BitmapFactory.decodeStream(inputStream);

        return myBitmap;

      }catch (Exception e){
        e.printStackTrace();
        return null;
      }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
      logoImageView.setImageBitmap(bitmap);
    }
  }

  public class DownloadTask extends AsyncTask<String,Void ,String>{

    @Override
    protected String doInBackground(String... urls) {
      String result = "";
      URL url;
      HttpURLConnection urlConnection = null;

      try{
        url = new URL(urls[0]);
        urlConnection = (HttpURLConnection) url.openConnection();

        //InputStream in = urlConnection.getInputStream();
        //InputStreamReader reader = new InputStreamReader(in);

        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null)
        {
          result += line;
        }
        /*int data = reader.read();
        int limit = 0;
        while(data != -1 ){
          if(limit > 80000){
            char current = (char) data;
            result += current;
            data = reader.read();
          }
          limit++;
        }*/
        return result;
      }catch (Exception e){
        e.printStackTrace();
        return null;
      }
    }

    @Override
    protected void onPostExecute(String s) {
      try{
        Pattern p2 = Pattern.compile("window._sharedData = (.*?);");
        //Log.i("TEXT",s);
        Matcher m2 = p2.matcher(s);

        m2.find();
        String jObject = m2.group(1);
        Log.i("MATCHER",jObject);
        JSONObject jsonObject = new JSONObject(jObject);
        JSONObject entry_data = jsonObject.getJSONObject("entry_data");
        JSONArray PostPage = entry_data.getJSONArray("PostPage");
        JSONObject first_graphql_shortcode_media = PostPage.getJSONObject(0).getJSONObject("graphql").getJSONObject("shortcode_media");
        JSONObject owner = first_graphql_shortcode_media.getJSONObject("owner");
        Log.i("USERNAME",owner.getString("username"));


        /*ImageDownloader imageTask = new ImageDownloader();
        Pattern p = Pattern.compile("<meta property=\"og:image\" content=\"(.*?)\"");
        Log.i("TEXT",s);
        Matcher m = p.matcher(s);

        m.find();
        Log.i("MATCHER",m.group(1));
        imageTask.execute(m.group(1));*/
      }catch (Exception e){
        downloadButton.setEnabled(false);
        logoImageView.setImageResource(R.mipmap.ic_launcher_round);
        e.printStackTrace();
      }

    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    downloadButton = findViewById(R.id.downloadButton);
    urlText = findViewById(R.id.urlText);
    logoImageView = findViewById(R.id.logoImageView);

    urlText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
          if(urlText.getText().length() > 0) {
            downloadButton.setEnabled(true);
            fetchPhoto();
          }else {
            downloadButton.setEnabled(false);
            logoImageView.setImageResource(R.mipmap.ic_launcher_round);
          }
      }
    });
  }

  public void pasteButton(View view){
    link = readFromClipboard();
    urlText.setText(link);
  }

  public String readFromClipboard() {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard.hasPrimaryClip()) {
      android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
      android.content.ClipData data = clipboard.getPrimaryClip();
      if (data != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
        return String.valueOf(data.getItemAt(0).getText());
    }
    return null;
  }

  public void fetchPhoto(){
    DownloadTask task = new DownloadTask();
    try {
      task.execute(link);
    }catch (Exception e){
      e.printStackTrace();
    }
  }
}
