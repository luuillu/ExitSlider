package com.lxw.android.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {
  protected static final String TAG = "luxinwei MainActivity";
  private static int sIndex = 0;
  private int mIndex = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mIndex = ++sIndex;

    setContentView(R.layout.activity_main);
    findViewById(R.id.hello).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent().setClass(MainActivity.this, SlideActivity.class));
      }
    });

    findViewById(R.id.hello_main).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent().setClass(MainActivity.this, MainActivity.class));
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.d(TAG, "onStart index:" + mIndex);
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    Log.d(TAG, "onRestart index:" + mIndex);
  }
  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume index:" + mIndex);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Log.d(TAG, "onPause index:" + mIndex);
  }

  protected void onStop() {
    super.onStop();
    Log.d(TAG, "onStop index:" + mIndex);
  }

  protected void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy index:" + mIndex);
  }
}
