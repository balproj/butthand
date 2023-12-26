package com.moc.button;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Map;

public class ListRecyclerAdapter extends RecyclerView.Adapter<ListRecyclerAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        RelativeLayout layout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_text);
            layout = itemView.findViewById(R.id.list_layout);
        }
    }

    private final ProfilesHelp profilesHelp;
    private final Map<String, String> names;
    private final List<String> list;

    public ListRecyclerAdapter(ProfilesHelp profilesHelp) {
        this.profilesHelp = profilesHelp;
        this.list = profilesHelp.getProfilesList();
        this.names = profilesHelp.getProfilesNames();
    }

    @NonNull
    @Override
    public ListRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ListRecyclerAdapter.ViewHolder holder, int position) {
        String id = list.get(position);
        String name = names.get(id);

        holder.name.setText(name);
        holder.layout.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle bundle = new Bundle();
                        bundle.putString("id", id);

                        Navigation.findNavController(holder.itemView)
                                .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);
        String id = list.get(fromPosition);

        list.remove(fromPosition);
        list.add(toPosition, id);
        profilesHelp.setProfilesList(list);
    }

    public void onItemDismiss(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getBindingAdapterPosition();
        String id = list.get(position);

        Snackbar snackbar = Snackbar.make(viewHolder.itemView, R.string.deleted, 5000);
        snackbar.setAction(R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.add(position, id);
                notifyItemInserted(position);
            }
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                    profilesHelp.removeProfile(id);
                }
            }
        });
        snackbar.show();

        list.remove(position);
        notifyItemRemoved(position);
    }
}
