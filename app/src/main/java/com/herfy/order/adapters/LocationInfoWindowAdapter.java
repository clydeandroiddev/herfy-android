package com.herfy.order.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.herfy.R;

public class LocationInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private View view;

    public LocationInfoWindowAdapter(Context context) {

        view = LayoutInflater.from(context).inflate(R.layout.info_window_location, null);

    }

    @Override
    public View getInfoWindow(Marker marker) {
        return getView(marker);
    }

    @Override
    public View getInfoContents(Marker marker) {
        return getView(marker);
    }

    public View getView(Marker marker) {

        TextView title = view.findViewById(R.id.title);
        title.setText(marker.getTitle());
        return view;
    }

}
