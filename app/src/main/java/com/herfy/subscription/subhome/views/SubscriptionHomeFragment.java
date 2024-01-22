package com.herfy.subscription.subhome.views;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.herfy.R;
import com.herfy.base.BaseFragment;
import com.herfy.constants.Text;
import com.herfy.subscription.premade.views.PreMadeStep1Activity;
import com.herfy.subscription.subhome.SubHomeContract;
import com.herfy.subscription.subhome.SubHomePresenter;
import com.herfy.subscription.subhome.adapter.SubHomeRecyclerViewAdapter;
import com.herfy.util.AnimUtil;
import com.herfy.util.CacheUtil;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SubscriptionHomeFragment extends BaseFragment implements SubHomeContract.View {

    private SubHomeContract.Presenter presenter;
    private SubHomeRecyclerViewAdapter adapter;
    private LinearLayoutManager linearLayoutManager;

    @BindView(R.id.list_subs)
    RecyclerView listSubs;
    @BindView(R.id.title)
    AppCompatTextView title;
    @BindView(R.id.blocker)
    View blocker;
    @BindView(R.id.new_sub_home_item_shimmer)
    ShimmerFrameLayout shimmerFrameLayout;

    public SubscriptionHomeFragment() {

    }

    public static SubscriptionHomeFragment newInstance() {
        return new SubscriptionHomeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        presenter = new SubHomePresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sub_home, container, false);
        ButterKnife.bind(this, view);
        setRetainInstance(true);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewAndApater();
        setupTranslations();
        blocker.setVisibility(View.VISIBLE);
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(() -> presenter.getHomeSubs(), 2000);
    }

    private void setupViewAndApater() {
        adapter = new SubHomeRecyclerViewAdapter(getActivity(), presenter, new ArrayList<>());
        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        listSubs.setLayoutManager(linearLayoutManager);
    }

    private void setupTranslations() {
        title.setText(CacheUtil.getInstance().getTranslations(Text.FILTER_OPTIONS, "FILTER OPTIONS"));
    }

    @Override
    public void showSubs(ArrayList<Integer> subs) {
        blocker.setVisibility(View.GONE);
        shimmerFrameLayout.setVisibility(View.GONE);
        adapter = new SubHomeRecyclerViewAdapter(getActivity(), presenter, subs);
        listSubs.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @OnClick(R.id.filter_button)
    void filter() {
        FilterOptionsDialogFragment fragment = FilterOptionsDialogFragment.newInstance();
        fragment.show(getChildFragmentManager(), FilterOptionsDialogFragment.class.getSimpleName());
    }

    @Override
    public void setSubItem(Integer item) {
        Intent intent = new Intent(getActivity(), PreMadeStep1Activity.class);
        startActivity(intent);
        getActivity().overridePendingTransition(AnimUtil.inF(), AnimUtil.inS());
    }

    @OnClick(R.id.blocker)
    void blocker() {
        // Do not remove
    }

    @Override
    public void openNutritionFacts() {
        Bundle args = new Bundle();
        args.putString("carbs", "62g");
        args.putString("fats", "42g");
        args.putString("calories", "8310");
        args.putString("protein", "25g");
        NutritionFactsDialogFragment fragment = NutritionFactsDialogFragment.newInstance();
        fragment.setArguments(args);
        fragment.show(getChildFragmentManager(), NutritionFactsDialogFragment.class.getSimpleName());
    }
}
