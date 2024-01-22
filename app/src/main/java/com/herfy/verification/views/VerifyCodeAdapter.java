package com.herfy.verification.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.util.StringUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VerifyCodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface VerifyCodeListener {
        void filled(String number, int position);
    }

    private VerifyCodeListener listener;

    private List<String> numbers;
    private Context context;
    private int textCount = 0;

    public VerifyCodeAdapter(Context context, List<String> numbers, VerifyCodeListener listener) {
        this.numbers = numbers;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {
        @BindView(R.id.otp)
        EditText otp;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_otp_view, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return numbers.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {

        TextWatcher codeTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                textCount = charSequence.length();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                holder.otp.removeTextChangedListener(this);
                holder.otp.setText(StringUtil.localize(holder.otp.getText().toString()));
                holder.otp.setSelection(editable.length());
                listener.filled(holder.otp.getText().toString(), position);
                if (position < (getItemCount() - 1) && !holder.otp.getText().toString().isEmpty()) {
                    try {
                        TextView nextField = (TextView) holder.otp.focusSearch(View.FOCUS_RIGHT);
                        nextField.requestFocus();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (position <= (getItemCount() - 1) && position != 0 && holder.otp.getText().toString().isEmpty()) {
                    TextView nextField = (TextView) holder.otp.focusSearch(View.FOCUS_LEFT);
                    nextField.requestFocus();
                }
                holder.otp.addTextChangedListener(this);
            }
        };

        holder.otp.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    if (position <= (getItemCount() - 1) && position != 0 && holder.otp.getText().toString().isEmpty()) {
                        TextView nextField = (TextView) holder.otp.focusSearch(View.FOCUS_LEFT);
                        nextField.requestFocus();
                    }
                } else {
                    if (position < (getItemCount() - 1) && !holder.otp.getText().toString().isEmpty()) {
                        try {
                            TextView nextField = (TextView) holder.otp.focusSearch(View.FOCUS_RIGHT);
                            nextField.requestFocus();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return false;
            }
        });

        holder.otp.addTextChangedListener(codeTextWatcher);

    }

}

