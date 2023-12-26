package com.moc.button;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.moc.button.databinding.FragmentListBinding;

import java.util.List;

public class ListFragment extends Fragment {
    private FragmentListBinding binding;
    private FragmentActivity activity;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    private ItemTouchHelper.SimpleCallback getItemTouchCallback(ListRecyclerAdapter adapter) {
        return new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.START | ItemTouchHelper.END) {
           final private ListRecyclerAdapter mAdapter = adapter;

            @Override
            public boolean onMove(@NonNull final RecyclerView recyclerView,
                                  @NonNull final RecyclerView.ViewHolder source,
                                  @NonNull final RecyclerView.ViewHolder target) {
                if (source.getItemViewType() != target.getItemViewType()) {
                    return false;
                }
                final int from = source.getBindingAdapterPosition();
                final int to = target.getBindingAdapterPosition();

                mAdapter.onItemMove(from, to);
                return true;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return true;
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder,
                                 final int swipeDir) {
                mAdapter.onItemDismiss(viewHolder);
            }
        };
    }

    private void createMenu(Context context) {
        activity.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.action_exit) {
                    activity.stopService(new Intent(activity, BackgroundService.class));
                    activity.finishAndRemoveTask();
                    return true;
                }
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ProfilesHelp profilesHelp = new ProfilesHelp(context);

                if (id == R.id.action_export) {
                    String data = profilesHelp.dumpProfiles();
                    if (data.equals("")) {
                        return true;
                    }
                    ClipData clip = ClipData.newPlainText("?", data);
                    clipboard.setPrimaryClip(clip);
                    return true;
                }
                else if (id == R.id.action_import) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip == null || clip.getItemCount() == 0) {
                        return false;
                    }
                    CharSequence data = clip.getItemAt(0).getText();
                    profilesHelp.loadProfiles(data.toString());
                    activity.recreate();
                    return true;
                }
                return false;
            }
        }, this.getViewLifecycleOwner());
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getContext();
        if (context == null) {
            return;
        }
        activity = getActivity();
        if (activity == null) {
            return;
        }
        createMenu(context);

        ProfilesHelp profilesHelp= new ProfilesHelp(context);
        List<String> profiles = profilesHelp.getProfilesList();
        if (profiles.size() < 1) {
            profilesHelp.loadProfiles(getResources().getString(R.string.default_profile));
        }

        ListRecyclerAdapter adapter = new ListRecyclerAdapter(profilesHelp);
        binding.items.setAdapter(adapter);
        binding.items.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(getItemTouchCallback(adapter));
        itemTouchHelper.attachToRecyclerView(binding.items);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                final EditText edittext = new EditText(context);

                alert.setTitle(R.string.create_title);
                alert.setView(edittext);

                alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String text = edittext.getText().toString();
                        if (text.equals("")) {
                            return;
                        }
                        String id = profilesHelp.addProfile(text, null);

                        Bundle bundle = new Bundle();
                        bundle.putString("id", id);

                        NavHostFragment.findNavController(ListFragment.this)
                                .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
                    }
                });
                alert.setNegativeButton(R.string.cancel, null);
                alert.show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}