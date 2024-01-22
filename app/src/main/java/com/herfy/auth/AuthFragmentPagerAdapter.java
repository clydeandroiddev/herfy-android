package com.herfy.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.herfy.auth.views.RegisterFragment;
import com.herfy.auth.views.SignInFragment;
import com.herfy.constants.Text;
import com.herfy.util.CacheUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AuthFragmentPagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<Fragment> fragments = new ArrayList<>();

    public AuthFragmentPagerAdapter(@NonNull FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        fragments.add(SignInFragment.newInstance());
        fragments.add(RegisterFragment.newInstance());
    }

    @NotNull
    @Override
    public Fragment getItem(int position) {
        if(position < fragments.size()) {
            return fragments.get(position);
        }
        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return CacheUtil.getInstance().getTranslations(Text.SIGN_IN);
            case 1:
                return CacheUtil.getInstance().getTranslations(Text.REGISTER, "REGISTER");
        }
        return super.getPageTitle(position);
    }

}
