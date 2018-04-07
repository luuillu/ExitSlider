package com.lxw.android.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.support.annotation.AttrRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

public class SlideActivity extends AppCompatActivity {
  static final String TAG = "luxinwei SlideActivity";
  private int mIndex = 0;
  private static int sIndex = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mIndex = ++sIndex;

    setContentView(R.layout.activity_main2);

    findViewById(R.id.hello).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent().setClass(SlideActivity.this, SlideActivity.class));
      }
    });


    if (!isTaskRoot()) {
      getWindow().setBackgroundDrawableResource(android.R.color.transparent);

      overridePendingTransition(R.anim.slide_in_right, 0);

      View contentView = ((ViewGroup)this.getWindow().getDecorView()).getChildAt(0);
      if (contentView.getBackground() == null) {
        contentView.setBackgroundResource(getTypedValue(this, android.R.attr.colorBackground).resourceId);
      }
      ExitSlider.attach(this, getResources().getColor(R.color.colorPrimaryDark));
    }
  }

  public static TypedValue getTypedValue(Context context, @AttrRes int attrId) {
    TypedValue typedValue = new TypedValue();
    context.getTheme().resolveAttribute(attrId, typedValue, true);
    return typedValue;
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
