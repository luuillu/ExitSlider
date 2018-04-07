package com.lxw.android.myapplication;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BlankFragment extends Fragment {
  private static final String TAG = "luxinwei BlankFragment";

  private int mIndex = 0;
  private static int sIndex = 0;

  public BlankFragment() {
    mIndex = ++sIndex;
    // Required empty public constructor
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    Log.d(TAG, "onAttach :" + mIndex);
  }

  @Override
  public void onCreate(Bundle savedInstance) {
    super.onCreate(savedInstance);
    Log.d(TAG, "onCreate :" + mIndex);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView :" + mIndex);
    return inflater.inflate(R.layout.fragment_blank, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstance) {
    super.onActivityCreated(savedInstance);
    Log.d(TAG, "onActivityCreated :" + mIndex);
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(TAG, "onStart :" + mIndex);
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume :" + mIndex);
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause :" + mIndex);
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d(TAG, "onStop :" + mIndex);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Log.d(TAG, "onDestroyView :" + mIndex);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy :" + mIndex);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    Log.d(TAG, "onDetach :" + mIndex);
  }
}
