package com.herfy.util;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.util.Property;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * <p>From the work of broady <a href=https://gist.github.com/broady/6314689>Animating Markers</a></p>
 * <p><a href=http://www.apache.org/licenses/LICENSE-2.0.html>Copyright 2013 Google Inc. Licensed under Apache 2.0</a></p>
 * <p>Created by Jessie-Futuristech on 10/13/2017.</p>
 */

public class MarkerAnimation
{
  public static void animateMarker(Marker marker, LatLng finalPosition,
                                   final LatLngInterpolator latLngInterpolator)
  {
    if (marker == null || finalPosition == null || latLngInterpolator == null)
    {
      return;
    }
    TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>()
    {
      @Override
      public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue)
      {
        return latLngInterpolator.interpolate(fraction, startValue, endValue);
      }
    };
    Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
    ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);
    animator.setDuration(5000);
    animator.start();
  }
}
