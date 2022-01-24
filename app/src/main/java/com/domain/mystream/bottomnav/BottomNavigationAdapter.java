package com.domain.mystream.bottomnav;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.domain.mystream.R;

/**
 * @author cubycode
 * @since 07/10/2018
 * All rights reserved
 */
public class BottomNavigationAdapter extends RecyclerView.Adapter<BottomNavigationAdapter.TabViewHolder> {

    private static final int NO_SELECTION = -1;

    private int selectedPosition = NO_SELECTION;

    private BottomNavigationListener listener;

    private Context context;

    public BottomNavigationAdapter(BottomNavigationListener listener, int selectedPosition) {
        this.listener = listener;
        this.selectedPosition = selectedPosition;
    }

    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_bottom_navigation_tab, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder viewHolder, int position) {
        BottomNavigationTab current = (BottomNavigationTab.values()[position]);

        viewHolder.iconIV.setImageResource(current.getIconRes());
        viewHolder.titleTV.setText(current.getMenuTitleRes());

        boolean isSelected = position == selectedPosition;
        int colorResId = isSelected ? R.color.selected_bottom_menu : R.color.unelected_bottom_menu;

        int color = ContextCompat.getColor(context, colorResId);
        viewHolder.titleTV.setTextColor(color);
        viewHolder.iconIV.getDrawable().setTint(color);
    }

    @Override
    public int getItemCount() {
        return BottomNavigationTab.values().length;
    }

    public void setSelectedItemPosition(int selectedPosition) {
        if (selectedPosition >= 0) {
            this.selectedPosition = selectedPosition;
            notifyDataSetChanged();
        }
    }

    private void onItemSelected(int selectedPosition) {
        boolean consumed = false;
        if (listener != null) {
            consumed = listener.onTabSelected(selectedPosition);
        }
        if (!consumed) {
            return;
        }

        int aux = this.selectedPosition;
        this.selectedPosition = selectedPosition;

        if (aux == this.selectedPosition) {
            return;
        }

        notifyItemChanged(aux);
        notifyItemChanged(this.selectedPosition);
    }

    class TabViewHolder extends RecyclerView.ViewHolder {

        ImageView iconIV;
        TextView titleTV;

        public TabViewHolder(View itemView) {
            super(itemView);
            iconIV = itemView.findViewById(R.id.ibnt_icon_iv);
            titleTV = itemView.findViewById(R.id.ibnt_title_tv);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemSelected(getAdapterPosition());
                }
            });
        }
    }

    public interface BottomNavigationListener {

        boolean onTabSelected(int pos);
    }
}
