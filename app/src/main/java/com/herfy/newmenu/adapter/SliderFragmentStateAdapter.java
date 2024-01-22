package com.herfy.newmenu.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.herfy.newmenu.SliderContract;
import com.herfy.newmenu.views.SliderFragment;
import com.skylinedynamics.solosdk.api.models.objects.Slide;

import java.util.ArrayList;

public class SliderFragmentStateAdapter extends FragmentStateAdapter {

    private ArrayList<Fragment> fragments = new ArrayList<>();

    public SliderFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity, SliderContract.Presenter sliderPresenter, ArrayList<Slide> slides) {
        super(fragmentActivity);
        for(Slide slide : slides) {
            fragments.add(SliderFragment.newInstance(sliderPresenter, slide));
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }
}
