package com.google.android.gms.samples.wallet.activity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.samples.wallet.adapter.TshirtCarouselAdapter;
import com.google.android.gms.samples.wallet.databinding.ActivityCarouselBinding;
import com.google.android.gms.samples.wallet.util.Json;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.samples.wallet.R;

public class CarouselActivity extends AppCompatActivity implements TshirtCarouselAdapter.OnItemClickListener {

  public static final String ITEM_ID_EXTRA = "itemIdExtra";

  private ActivityCarouselBinding layoutBinding;
  private RecyclerView.Adapter adapter;
  private RecyclerView.LayoutManager layoutManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initializeUi();
  }

  private void initializeUi() {

    // Use view binding to access the UI elements
    layoutBinding = ActivityCarouselBinding.inflate(getLayoutInflater());
    setContentView(layoutBinding.getRoot());

    layoutBinding.recyclerView.setHasFixedSize(true);
    layoutManager = new GridLayoutManager(this, 2);
    layoutBinding.recyclerView.setLayoutManager(layoutManager);
    adapter = new TshirtCarouselAdapter(this, Json.readFromResources(this, R.raw.tshirts), this);

    layoutBinding.recyclerView.setAdapter(adapter);
  }

  @Override
  public void onItemClick(String itemId) {
    Intent intent = new Intent(this, CheckoutActivity.class);
    intent.putExtra(ITEM_ID_EXTRA, itemId);
    startActivity(intent);
  }
}