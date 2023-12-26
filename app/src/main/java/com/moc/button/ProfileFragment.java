package com.moc.button;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends PreferenceFragmentCompat {
    private static final String TAG = ProfileFragment.class.getName();

    private Context context;
    private ProfilesHelp profilesHelp;
    private Map<String, String> key_name = null;
    private Map<String, String> profiles_names = null;

    private void keyNameInit() {
        this.key_name = new HashMap<>();
        String[] names = getResources().getStringArray(R.array.action_entries);
        String[] keys = getResources().getStringArray(R.array.action_values);

        for (int i = 0; i < keys.length; i++) {
            key_name.put(keys[i], names[i]);
        }
    }

    private void checkOverlayPermission(Map<String, String> map) {
        if (map.containsKey("act_intent")) {
            String target = map.get("param_intent_target");
            if (target == null || !target.equals("activity")) {
                return;
            }
        } else return;

        if (Settings.canDrawOverlays(context)) {
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(R.string.permission_required);
        alert.setMessage(R.string.request_permission);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                startActivity(intent);
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.show();
    }


    private Map<String, String> parseLinearLayout(LinearLayout layout) {
        Map<String, String> map = new HashMap<>();

        int count = layout.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = layout.getChildAt(i);

            if (child.getVisibility() == View.GONE) {
                continue;
            }
            String tag = (String) child.getTag();

            if (child instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) child;
                if (checkBox.isChecked() && !tag.equals("")) {
                    map.put(tag, "true");
                }
            }
            else if (child instanceof SwitchCompat) {
                SwitchCompat aSwitch = (SwitchCompat) child;
                if (aSwitch.isChecked() && !tag.equals("")) {
                    map.put(tag, "true");
                }
            }
            else if (child instanceof EditText) {
                EditText editText = (EditText) child;
                String text = editText.getText().toString();
                map.put(tag, text);
            }
            else if (child instanceof RadioGroup) {
                RadioGroup radioGroup = (RadioGroup) child;
                int id = radioGroup.getCheckedRadioButtonId();
                RadioButton radioButton = radioGroup.findViewById(id);
                if (radioButton == null) {
                    continue;
                }
                String text = (String) radioButton.getTag();
                map.put(tag, text);
            }
            else if (child instanceof LinearLayout) {
                map.putAll(parseLinearLayout((LinearLayout) child));
            }
        }
        return map;
    }


    void loadLinearLayout(LinearLayout layout, Map<String, String> map) {
        int count = layout.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = layout.getChildAt(i);
            String tag = (String) child.getTag();

            if (child instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) child;
                if (map.containsKey(tag)) {
                    checkBox.setChecked(true);
                }
            }
            else if (child instanceof SwitchCompat) {
                SwitchCompat aSwitch = (SwitchCompat) child;
                if (map.containsKey(tag)) {
                    aSwitch.setChecked(true);
                }
            }
            else if (child instanceof EditText) {
                EditText editText = (EditText) child;
                if (map.containsKey(tag)) {
                    editText.setText(map.get(tag));
                }
            }
            else if (child instanceof RadioGroup) {
                RadioGroup radioGroup = (RadioGroup) child;
                String val = map.get(tag);
                if (val == null)
                    continue;
                int c = radioGroup.getChildCount();
                for (int o = 0; o < c; o++) {
                    RadioButton radioButton = (RadioButton) radioGroup.getChildAt(o);
                    String button_tag = (String) radioButton.getTag();
                    if (val.equals(button_tag)) {
                        radioButton.setChecked(true);
                        break;
                    }
                }
            }
            else if (child instanceof LinearLayout) {
                loadLinearLayout((LinearLayout) child, map);
            }
        }
    }


    String genSummary(Map<String, String> map) {
        List<String> list = new ArrayList<>();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String name = key_name.get(key);

            if (key.equals("act_switch")) {
                String id = map.get("param_switch");
                String sw_name = profiles_names.get(id);

                if (sw_name != null) {
                    list.add("-> " + sw_name);
                }
            }
            else if (key.equals("act_vibrate")) {
                String duration = map.get("param_vibrate");
                list.add(name + ": " + duration);
            }
            else if (name != null) {
                list.add(name);
            }
        }
        return String.join("\n", list);
    }

    private void actionsDialog(Preference preference) {
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_actions, null);

        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        if (preferences == null) {
            Log.e(TAG, "preferences is null");
            return;
        }
        String key = preference.getKey();
        Map<String, String> map = profilesHelp.sharedGetMap(preferences, key);

        // Setup handlers list
        List<String> profiles = profilesHelp.getProfilesList();

        LinearLayout swl = v.findViewById(R.id.switch_linear);
        RadioGroup rg =  swl.findViewById(R.id.param_switch);

        for (String id : profiles) {
            RadioButton rb = new RadioButton(context);

            rb.setText(profiles_names.get(id));
            rb.setTextSize(18);

            rb.setTag(id);
            rg.addView(rb);
        }
        //
        LinearLayout main_layout = v.findViewById(R.id.actions_list);
        loadLinearLayout(main_layout, map);

        // Setup hidden layouts
        int [][]ids = {
                {R.id.act_vibrate, R.id.vibrate_linear},
                {R.id.act_wakelock_acquire, R.id.wakelock_linear},
                {R.id.act_switch, R.id.switch_linear},
                {R.id.act_intent, R.id.intent_linear}
        };
        for (int []idm : ids) {
            CheckBox checkBox = v.findViewById(idm[0]);
            LinearLayout layout = v.findViewById(idm[1]);

            if (checkBox.isChecked()) {
                layout.setVisibility(View.VISIBLE);
            }
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    layout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }
        //
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(R.string.actions);
        alert.setView(v);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Map<String, String> map = parseLinearLayout(main_layout);
                profilesHelp.sharedSetMap(preferences, preference.getKey(), map);

                preference.setSummary(genSummary(map));
                checkOverlayPermission(map);
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.show();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        this.context = getContext();
        if (context == null) {
            return;
        }
        Bundle bundle = getArguments();
        if (bundle == null) {
            Log.e(TAG, "getArguments is null");
            return;
        }
        String id = bundle.getString("id");

        this.profilesHelp = new ProfilesHelp(context);
        keyNameInit();
        this.profiles_names = profilesHelp.getProfilesNames();

        getPreferenceManager().setSharedPreferencesName(id);
        setPreferencesFromResource(R.xml.profile_preferences, rootKey);

        EditTextPreference editor = findPreference("idle_dur");
        if (editor != null) {
            editor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object val) {
                    try {
                        Integer.parseInt(val.toString());
                    } catch (NumberFormatException e) {
                        View v = getView();
                        if (v != null) {
                            Snackbar.make(v, "Invalid value", Toast.LENGTH_SHORT).show();
                        }
                        return false;
                    }
                    return true;
                }
            });
        }

        final String[] action_pref = {
                "press_up_action",  "press_down_action",
                "release_up_action", "release_down_action",
                "timeout_idle_action"
        };
        for (String key : action_pref) {
            Preference pre = findPreference(key);
            if (pre == null) {
                Log.e(TAG, "findPreference is null: " + key);
                continue;
            }
            SharedPreferences shared = pre.getSharedPreferences();
            if (shared == null) {
                Log.e(TAG, "getSharedPreferences is null: " + key);
                continue;
            }

            String summary = genSummary(profilesHelp.sharedGetMap(shared, pre.getKey()));
            pre.setSummary(summary);

            pre.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    actionsDialog(preference);
                    return false;
                }
            });
        }
    }
}