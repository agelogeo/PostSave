package com.agelogeo.postsave;

import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
  TextView urlText;
  Button downloadButton;
  ImageView logoImageView;

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
            logoImageView.setImageResource(R.drawable.error);
          }else {
            downloadButton.setEnabled(false);
            logoImageView.setImageResource(R.mipmap.ic_launcher_round);
          }
      }
    });
  }

  public void pasteButton(View view){
    urlText.setText(readFromClipboard());
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
}
