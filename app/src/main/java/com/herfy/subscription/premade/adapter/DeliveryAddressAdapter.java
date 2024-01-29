package com.herfy.subscription.premade.adapter;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.data.constants.Text;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.herfy.util.AuthUtil;
import com.herfy.util.CacheUtil;
import com.herfy.views.DynamicTheme;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeliveryAddressAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface DeliveryAddressListener {
        void selectDeliveryAddress(Address address, int position, String name);
    }

    private DeliveryAddressListener listener;

    private List<Address> menus;
    private Context context;
    public String selectedId = "";

    public DeliveryAddressAdapter(Context context, List<Address> menus, DeliveryAddressListener listener) {
        this.menus = menus;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.address_main)
        MaterialCardView addressMain;
        @BindView(R.id.check_image)
        ImageView checkImage;
        @BindView(R.id.address_name)
        AppCompatTextView addressName;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_address_delivery, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return menus.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {

        final Address menu = menus.get(position);

        String colorStr = DynamicTheme.getStringColor(context);
        String deliverTo = CacheUtil.getInstance().getTranslations(Text.DELIVER_TO, "Deliver to");
        String label = menu.getAttributes().getLabel();
        if (label.isEmpty()) {
            label = CacheUtil.getInstance().getTranslations(Text.PLACE, "Place");
        }
        String name = AuthUtil.getInstance().getCustomer().getAttributes().getFirstName() + "\'s " +
                label;
        holder.addressName.setText(Html.fromHtml("<font color=" + colorStr + ">" + deliverTo + "</font>") + " " +
                name);

        holder.addressMain.setOnClickListener(v -> {
            listener.selectDeliveryAddress(menu, position, name);
        });

        if (selectedId.equals(menu.getId())) {
            holder.checkImage.setImageResource(R.drawable.ic_picked_payment_type_svg);
        } else {
            holder.checkImage.setImageResource(R.drawable.ic_rectangle_47);
        }


    }

}
