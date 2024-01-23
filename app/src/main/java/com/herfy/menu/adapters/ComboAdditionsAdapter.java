package com.herfy.menu.adapters;

import android.app.Activity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.menu.MenuContract;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.herfy.util.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ComboAdditionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public int promoWidth = 200;
    public String sizeId;
    private List<ModifierItem> menuItemComponents;
    private Activity context;
    private MenuContract.Presenter presenter;
    private ModifierGroup modifierGroup;
    private ComboAdditionsListener listener;
    private ComboAdditionsAdapter comboAdditionsAdapter;

    public ComboAdditionsAdapter(Activity context, List<ModifierItem> menuItemComponents, MenuContract.Presenter presenter, ModifierGroup modifierGroup, ComboAdditionsListener listener) {
        this.menuItemComponents = menuItemComponents;
        this.context = context;
        this.listener = listener;
        this.modifierGroup = modifierGroup;
        this.presenter = presenter;
        comboAdditionsAdapter = this;
    }

    @Override
    public int getItemCount() {
        return menuItemComponents.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {
        final ModifierItem modifierItem = menuItemComponents.get(position);

        if (modifierItem.getAttributes().isSelected()) {
            holder.check.setImageResource(R.drawable.modifier_item_selected);
        } else {
            holder.check.setImageResource(R.drawable.modifier_item_unselected);
        }

        holder.checkLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modifierItem.getAttributes().isSelected()) {
                    listener.onCheckChanged(false, modifierGroup, modifierItem, position, comboAdditionsAdapter);
                } else {
                    listener.onCheckChanged(true, modifierGroup, modifierItem, position, comboAdditionsAdapter);
                }
            }
        });

        String imageUrl = modifierItem.getAttributes().getImage();
        holder.image.setVisibility(View.VISIBLE);
        Glide.with(context).load(imageUrl).apply(RequestOptions.centerInsideTransform()).placeholder(R.drawable.imagepicker_image_placeholder).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.image);

        String price;
        if(LocaleUtil.getInstance().isArabic()){
            price = (StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()) + "+");
        }else {
            price = ("+" + StringUtil.getFormattedPrice(modifierItem.getAttributes().getPrice()));
        }

        String text = "<font color='#D62300'>" + price + "</font>";
        if (modifierItem.getAttributes().getPrice() <= 0) {
            text = "";
        }
        holder.modifierNameAndPrice.setText(Html.fromHtml(modifierItem.getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage()) + " " + text), TextView.BufferType.SPANNABLE);

        holder.quantity.setText(StringUtil.localize(String.valueOf(modifierItem.getAttributes().getQuantity())));

        holder.quantityContainer.setVisibility(modifierGroup.getAttributes().
                getMaximum() > 1 && modifierItem.getAttributes().
                isSelected() && modifierItem.getAttributes().
                getMaximum() > 1 ? View.VISIBLE : View.GONE);

        holder.quantityDown.setClickable(modifierItem.getAttributes().getQuantity() != 1);

        if (modifierItem.getAttributes().getQuantity() != 1) {
            holder.quantityUp.setImageResource(R.drawable.quantity_up_orange);
        } else {
            holder.quantityDown.setImageResource(R.drawable.quantity_down_gray);
        }

        holder.quantityDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onQuantityChanged(false, modifierGroup, modifierItem, position, comboAdditionsAdapter);
            }
        });

        holder.quantityUp.setClickable(modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum());

        if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
            holder.quantityUp.setImageResource(R.drawable.quantity_up_orange);
        } else {
            holder.quantityDown.setImageResource(R.drawable.quantity_down_gray);
        }

        holder.quantityUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    if (modifierItem.getAttributes().getQuantity() < modifierItem.getAttributes().getMaximum()) {
                        listener.onQuantityChanged(true, modifierGroup, modifierItem, position, comboAdditionsAdapter);
                    } else {
                        Toast.makeText(context, CacheUtil.getInstance().getTranslations(Text.MODIFIER_MAXIMUM_LIMIT), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_modifier_group, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView((ViewHolderData) holder, position);
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.container)
        ConstraintLayout container;
        @BindView(R.id.check)
        ImageView check;
        @BindView(R.id.check_layout)
        FrameLayout checkLayout;
        @BindView(R.id.image)
        ImageView image;
        @BindView(R.id.modifierNameAndPrice)
        TextView modifierNameAndPrice;
        @BindView(R.id.quantity)
        TextView quantity;
        @BindView(R.id.quantity_container)
        ConstraintLayout quantityContainer;
        @BindView(R.id.quantityDown)
        ImageButton quantityDown;
        @BindView(R.id.quantityUp)
        ImageButton quantityUp;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    public interface ComboAdditionsListener {
        void onCheckChanged(boolean isSelected, ModifierGroup modifierGroup, ModifierItem modifierItem, int position, ComboAdditionsAdapter additionsAdapter);

        void onQuantityChanged(boolean add, ModifierGroup modifierGroup, ModifierItem modifierItem, int position, ComboAdditionsAdapter additionsAdapter);
    }

}
