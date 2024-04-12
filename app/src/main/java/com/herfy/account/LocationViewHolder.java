package com.herfy.account;

import android.content.DialogInterface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Address;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LocationViewHolder extends RecyclerView.ViewHolder implements AccountContract.View {

    private AccountContract.Presenter accountPresenter;

    @BindView(R.id.type) TextView type;
    @BindView(R.id.address) TextView address;
	@BindView(R.id.zip) TextView zip;

    @BindView(R.id.delete) ImageView delete;

    public LocationViewHolder(AccountContract.Presenter accountPresenter, boolean isEdit, View itemView) {

        super(itemView);

        ButterKnife.bind(this, itemView);
		delete.setVisibility(isEdit ? View.VISIBLE : View.GONE);

		this.accountPresenter = accountPresenter;

        setupViews();
        setupFonts();
        setupTranslations();

    }

    @Override
    public void setPresenter(AccountContract.Presenter presenter) {
        accountPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void setCartExpiry() {

    }

    @Override
    public void setupViews() {

    }

    public void setupFonts() {

    }

    @Override
    public void setupTranslations() {

    }

    @Override
    public void showCustomer(String firstName, String lastName, String gender, String phoneNumber, String email, String provider) {

    }

    @Override
    public void showDateOfBirth(Date birthDate, String date) {

    }

    @Override
    public void showEmail(boolean show) {

    }

    @Override
    public void showPassword(boolean show) {

    }

    @Override
    public void showErrorFirstName(String error) {

    }

    @Override
    public void showErrorLastName(String error) {

    }

    @Override
    public void showErrorPhoneNumber(String error) {

    }

    @Override
    public void showErrorEmail(String error) {

    }

    @Override
    public void showErrorNewPassword(String error) {

    }

    @Override
    public void showErrorConfirmPassword(String error) {

    }

    @Override
    public void showErrorConfirmNewPassword(String error) {

    }

    @Override
    public void showPasswordUpdated() {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void showAccount() {

    }

    @Override
    public void showLocations(ArrayList<Address> locations) {

    }

	@Override
	public void hideLocations() {

	}

	@Override
    public void setLocation(Address location) {

        type.setText(location.getAttributes().getLabel());
        address.setText(location.getAttributes().getLine1());

        String instructionsValue = location.getAttributes().getInstructions();

        if(instructionsValue != null && !instructionsValue.isEmpty()) {
            zip.setText(instructionsValue);
			zip.setVisibility(View.VISIBLE);
        } else {
			zip.setVisibility(View.GONE);
        }

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((BaseActivity) itemView.getContext()).showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.ARE_YOU_SURE_YOU_WANT_TO_DELETE_ADDRESS, "Are you sure you want to delete this address?"), CacheUtil.getInstance().getTranslations(Text.DELETE), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        ((BaseActivity) itemView.getContext()).showLoadingDialog();
                        accountPresenter.deleteLocation(location);
                    }
                }, CacheUtil.getInstance().getTranslations(Text.CANCEL), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
            }
        });

    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void accountDeleted() {

    }

    @Override
    public void showCustomerCards() {

    }

    @Override
    public void showRevelRewardPoints(Double pointValue) {

    }

    @Override
    public void showCard(boolean isDefault) {
    }

}
