package com.herfy.newmenu;

import androidx.recyclerview.widget.RecyclerView;

import com.herfy.newmenu.viewholder.NewMenuViewHolder;
import com.herfy.newmenu.views.ScrollDirection;

public interface NewMenuTabListener {
    void setTabs(RecyclerView.Adapter<NewMenuViewHolder> adapter);
    void onMenuCategoryItemsScrolled(boolean show, boolean isLastItemVisible);
    void onLastItemReach(boolean isScrollable, ScrollDirection scrollDirection);
}
