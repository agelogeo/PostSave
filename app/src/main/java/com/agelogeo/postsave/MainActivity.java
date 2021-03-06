package com.agelogeo.postsave;

import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
  TextView urlText;
  Button downloadButton;
  ImageView logoImageView;
  String link;
  GridView imageGrid;
  ArrayList<Bitmap> bitmapList;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    LayoutInflater inflator = LayoutInflater.from(this);
    View v = inflator.inflate(R.layout.bar, null);

    ActionBar actionBar = getSupportActionBar(); // or getActionBar();
      //actionBar.setTitle("UltimateSaver");
    ActionBar.LayoutParams p = new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    p.gravity = Gravity.CENTER;

    try{
      actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
      actionBar.setCustomView(R.layout.bar);

    }catch (Exception e){
        e.printStackTrace();
    }

    downloadButton = findViewById(R.id.downloadButton);
    urlText = findViewById(R.id.urlText);

    imageGrid = findViewById(R.id.gridview);
    bitmapList = new ArrayList<Bitmap>();

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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle presses on the action bar items
    switch (item.getItemId()) {
      case R.id.instagram:
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if (launchIntent != null) {
          startActivity(launchIntent);//null pointer check in case package name was not found
        }else{
            Toast.makeText(getApplicationContext(),"Instagram is not installed.",Toast.LENGTH_SHORT).show();
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
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

  public void analyzePost(String s){
    try{
      String link ;
      Pattern pattern = Pattern.compile("window._sharedData = (.*?)[}];");
      Matcher matcher = pattern.matcher(s);

      matcher.find();
      String jObject = matcher.group(1)+"}";
      JSONObject jsonObject = new JSONObject(jObject);
      JSONObject entry_data = jsonObject.getJSONObject("entry_data");
      JSONArray PostPage = entry_data.getJSONArray("PostPage");
      JSONObject first_graphql_shortcode_media = PostPage.getJSONObject(0).getJSONObject("graphql").getJSONObject("shortcode_media");
      JSONObject owner = first_graphql_shortcode_media.getJSONObject("owner");

      Log.i("USERNAME",owner.getString("username"));
      Log.i("PROFILE_PIC_URL",owner.getString("profile_pic_url"));

      if(first_graphql_shortcode_media.has("edge_sidecar_to_children")){
        JSONArray children_edges = first_graphql_shortcode_media.getJSONObject("edge_sidecar_to_children").getJSONArray("edges");
        Log.i("WITH_CHILDREN_COUNT",Integer.toString(children_edges.length()));

        for(int i=0; i<children_edges.length(); i++){
          JSONObject node = children_edges.getJSONObject(i).getJSONObject("node");

          if(node.has("video_url")){
            //link = node.getString("video_url");
            link = node.getJSONArray("display_resources").getJSONObject(2).getString("src");
            Log.i("CHILDREN_W_VIDEO_"+(i+1),node.getString("video_url"));
          }else{
            link = node.getJSONArray("display_resources").getJSONObject(2).getString("src");
            Log.i("CHILDREN_W_PHOTO_"+(i+1),node.getJSONArray("display_resources").getJSONObject(2).getString("src"));
          }
          ImageDownloader imageTask = new ImageDownloader();
          imageTask.execute(link);
        }
      }else{
        if(first_graphql_shortcode_media.has("video_url")){
          Log.i("NO_CHILDREN_W_VIDEO",first_graphql_shortcode_media.getString("video_url"));
          //first_graphql_shortcode_media.getString("video_url");
          link = first_graphql_shortcode_media.getJSONArray("display_resources").getJSONObject(2).getString("src");
        }else{
          Log.i("NO_CHILDREN_W_PHOTO",first_graphql_shortcode_media.getJSONArray("display_resources").getJSONObject(2).getString("src"));
          link = first_graphql_shortcode_media.getJSONArray("display_resources").getJSONObject(2).getString("src");
        }
        ImageDownloader imageTask = new ImageDownloader();
        imageTask.execute(link);
      }
    }catch (Exception e){
      downloadButton.setEnabled(false);
      e.printStackTrace();
    }
  }

  public void fetchPhoto(){
    DownloadTask task = new DownloadTask();
    try {
      task.execute(link);
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  public class DownloadTask extends AsyncTask<String,Void ,String>{

    @Override
    protected String doInBackground(String... urls) {
      String result = "",line = null;
      URL url;
      HttpURLConnection urlConnection = null;

      try{
        url = new URL(urls[0]);
        urlConnection = (HttpURLConnection) url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        while ((line = reader.readLine()) != null)
          result += line;

        return result;
      }catch (Exception e){
        e.printStackTrace();
        return null;
      }
    }

    @Override
    protected void onPostExecute(String s) {
      analyzePost(s);
    }
  }

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
      bitmapList.add(bitmap);
      imageGrid.setAdapter(new ImageAdapter(getApplicationContext(), bitmapList));

    }
  }

}
