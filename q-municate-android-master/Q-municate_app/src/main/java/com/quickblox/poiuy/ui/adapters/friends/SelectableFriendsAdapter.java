package com.quickblox.poiuy.ui.adapters.friends;

import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.quickblox.poiuy.R;
import com.quickblox.poiuy.ui.activities.base.BaseActivity;
import com.quickblox.poiuy.ui.adapters.base.BaseClickListenerViewHolder;
import com.quickblox.poiuy.utils.listeners.SelectUsersListener;
import com.quickblox.q_municate_core.utils.ChatUtils;
import com.quickblox.q_municate_user_service.model.QMUser;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

public class SelectableFriendsAdapter extends FriendsAdapter {

    private SelectUsersListener selectUsersListener;
    private int counterFriends;
    private List<QMUser> selectedFriendsList;
    private SparseBooleanArray sparseArrayCheckBoxes;

    public SelectableFriendsAdapter(BaseActivity baseActivity, List<QMUser> userList, boolean withFirstLetter) {
        super(baseActivity, userList, withFirstLetter);
        selectedFriendsList = new ArrayList<QMUser>();
        sparseArrayCheckBoxes = new SparseBooleanArray(userList.size());
    }

    @Override
    public BaseClickListenerViewHolder<QMUser> onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(this, layoutInflater.inflate(R.layout.item_friend_selectable, parent, false));
    }

    @Override
    public void onBindViewHolder(BaseClickListenerViewHolder<QMUser> baseClickListenerViewHolder, final int position) {
        super.onBindViewHolder(baseClickListenerViewHolder, position);

        final QMUser user = getItem(position);
        final ViewHolder viewHolder = (ViewHolder) baseClickListenerViewHolder;

        viewHolder.selectFriendCheckBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                sparseArrayCheckBoxes.put(position, checkBox.isChecked());
                addOrRemoveSelectedFriend(checkBox.isChecked(), user);
                notifyCounterChanged(checkBox.isChecked());
            }
        });

        boolean checked = sparseArrayCheckBoxes.get(position);
        viewHolder.selectFriendCheckBox.setChecked(checked);
    }

    public void setSelectUsersListener(SelectUsersListener listener) {
        selectUsersListener = listener;
    }

    public void selectFriend(int position) {
        boolean checked = !sparseArrayCheckBoxes.get(position);
        sparseArrayCheckBoxes.put(position, checked);
        addOrRemoveSelectedFriend(checked, getItem(position));
        notifyCounterChanged(checked);
        notifyItemChanged(position);
    }

    private void addOrRemoveSelectedFriend(boolean checked, QMUser user) {
        if (checked) {
            selectedFriendsList.add(user);
        } else if (selectedFriendsList.contains(user)) {
            selectedFriendsList.remove(user);
        }
    }

    private void notifyCounterChanged(boolean isIncrease) {
        changeCounter(isIncrease);
        if (selectUsersListener != null) {
            String fullNames = ChatUtils.getSelectedFriendsFullNamesFromMap(selectedFriendsList);
            selectUsersListener.onSelectedUsersChanged(counterFriends, fullNames);
        }
    }

    private void changeCounter(boolean isIncrease) {
        if (isIncrease) {
            counterFriends++;
        } else {
            counterFriends--;
        }
    }

    public void clearSelectedFriends() {
        sparseArrayCheckBoxes.clear();
        selectedFriendsList.clear();
        counterFriends = 0;
        notifyDataSetChanged();
    }

    public List<QMUser> getSelectedFriendsList() {
        return selectedFriendsList;
    }

    protected static class ViewHolder extends FriendsAdapter.ViewHolder {

        @Bind(R.id.selected_friend_checkbox)
        CheckBox selectFriendCheckBox;

        public ViewHolder(FriendsAdapter adapter, View view) {
            super(adapter, view);
        }
    }
}