package com.google.android.gms.samples.wallet.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.samples.wallet.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TshirtCarouselAdapter extends RecyclerView.Adapter<TshirtCarouselAdapter.ViewHolder> {

  private Context context;
  private JSONArray garments;
  private OnItemClickListener clickListener;

  public TshirtCarouselAdapter(Context context, JSONArray garments, OnItemClickListener clickListener) {
    this.context = context;
    this.garments = garments;
    this.clickListener = clickListener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View itemView = LayoutInflater
        .from(parent.getContext())
        .inflate(R.layout.carousel_item_view, parent, false);

    ViewHolder vh = new ViewHolder(itemView);
    return vh;
  }

  @Override
  public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

    try {
      final JSONObject garment = garments.getJSONObject(position);

      final String imageUri = String.format("@drawable/%s", garment.getString("image"));
      final int imageResource = context.getResources()
          .getIdentifier(imageUri, null, context.getPackageName());
      holder.garmentImage.setImageResource(imageResource);

      holder.garmentName.setText(garment.getString("title"));
      holder.garmentPrice.setText(
          String.format(Locale.getDefault(), "$%.2f", garment.getDouble("price")));

      final OnItemClickListener clickListener = this.clickListener;
      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          try {
            final JSONObject currentGarment = garments.getJSONObject(holder.getAdapterPosition());
            clickListener.onItemClick(currentGarment.getString("image"));
          } catch (JSONException ignored) {
          }
        }
      });

    } catch(JSONException ignored) {
    }
  }

  @Override
  public int getItemCount() {
    return garments.length();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ImageView garmentImage;
    public TextView garmentName;
    public TextView garmentPrice;

    public ViewHolder(final View itemView){
      super(itemView);
      garmentImage = (ImageView) itemView.findViewById(R.id.garment_image);
      garmentName = (TextView) itemView.findViewById(R.id.garment_name);
      garmentPrice = (TextView) itemView.findViewById(R.id.garment_price);
    }
  }

  public interface OnItemClickListener {
    void onItemClick(String itemId);
  }
}
