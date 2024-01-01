package com.moc.button;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ProfilesHelp {
    private static final String TAG = ProfilesHelp.class.getName();

    final private Context context;
    final private SharedPreferences main_pref;

    public ProfilesHelp(Context context) {
        this.context = context;
        this.main_pref = context.getSharedPreferences("last", 0);
    }

    boolean loadProfiles(String str) {
        Map<String, String> profiles = StringToMap(str);
        if (profiles.size() == 0) {
            return false;
        }
        for (Map.Entry<String, String> entry : profiles.entrySet()) {
            String id = entry.getKey();
            Map<String, String> map = StringToMap(entry.getValue());
            if (map.size() == 0) {
                return false;
            }
            String title = map.get("title");
            if (title == null) {
                return false;
            }
            addProfile(title, id);

            SharedPreferences.Editor editor = getProfile(id).edit();
            for (Map.Entry<String, String> e: map.entrySet()) {
                editor.putString(e.getKey(), e.getValue());
            }
            editor.apply();
        }
        return true;
    }

    String dumpProfiles() {
        Map<String, String> map = new LinkedHashMap<>();

        List<String> profiles = getProfilesList();
        for (String id : profiles) {
            SharedPreferences pref = getProfile(id);
            Map<String, String> all = (Map<String, String>) pref.getAll();
            String encoded = MapToString(all);
            map.put(id, encoded);
        }
        return MapToString(map);
    }

    String MapToString(Map<String, String> map) {
        String str = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey(), value = entry.getValue();
            str += key.length() + ":" + key;
            str += value.length() + ":" + value;
        }
        return str;
    }

    Map<String, String> StringToMap(String str) {
        Map<String, String> map = new LinkedHashMap<>();
        String key = null, value;
        while (true) {
            String[] s = str.split(":", 2);
            if (s.length < 2)
                break;
            int size;
            try { size = Integer.parseInt(s[0].trim());
            } catch (NumberFormatException e) {
                break;
            }
            if (s[1].length() < size) {
                break;
            }
            value = s[1].substring(0, size);
            if (key == null) {
                key = value;
            } else {
                map.put(key, value);
                key = null;
            }
            str = s[1].substring(size);
            if (str.length() == 0)
                break;
        }
        return map;
    }

    Map<String, String> sharedGetMap(SharedPreferences shared, String key) {
        String str = shared.getString(key, "");
        return StringToMap(str);
    }

    void sharedSetMap(SharedPreferences shared, String key, Map<String, String> map) {
        String str = MapToString(map);
        shared.edit().putString(key, str).apply();
    }

    List<String> getProfileListEx(String tag) {
        String poss = main_pref.getString(tag, "");
        List<String> list = new ArrayList<>();
        if (poss.equals("")) {
            return list;
        }
        list.addAll(Arrays.asList(poss.split(",")));
        return list;
    }

    void setProfileListEx(String tag, List<String> list) {
        String poss = String.join(",", list);
        main_pref.edit().putString(tag, poss).apply();
    }

    List<String> getProfilesList() {
        return getProfileListEx("list");
    }

    void setProfilesList(List<String> list) {
        setProfileListEx("list", list);
    }

    Map<String, String> getProfilesNames() {
        List<String> list = getProfilesList();
        Map<String, String> names = new HashMap<>();
        for (String id : list) {
            SharedPreferences pref = getProfile(id);
            names.put(id, pref.getString("title", "unknown"));
        }
        return names;
    }

    SharedPreferences getProfile(String name) {
       List<String> list = getProfilesList();
        if (!list.contains(name)) {
            Log.e(TAG, "profile '" + name + "' not found");
            return null;
        }
        SharedPreferences pref = context.getSharedPreferences(name, 0);
        return pref;
    }

    void setProfileName(String id, String name) {
        SharedPreferences pref = getProfile(id);
        pref.edit().putString("title", name).apply();
        //main_pref.edit().putString(id, name).apply();
    }

    String addProfile(String name, String id) {
        List<String> free = getProfileListEx("free");
        List<String> list = getProfilesList();

        if (id != null) {
            if (free.contains(id)) {
                free.remove(id);
                setProfileListEx("free", free);
            }
        }
        else if (free.size() == 0) {
            int num = main_pref.getInt("num", 0);
            do {
                num += 1;
                id = String.valueOf(num);
            } while (list.contains(id) || free.contains(id));
            main_pref.edit().putInt("num", num).apply();
        } else {
            id = free.get(0);
            free.remove(0);
            setProfileListEx("free", free);
        }
        if (!list.contains(id)) {
            list.add(id);
            setProfilesList(list);
        }
        setProfileName(id, name);
        return id;
    }

    void removeProfile(String name) {
        List<String> list = getProfilesList();

        if (!list.contains(name)) {
            Log.e(TAG, "profile '" + name + "' not found");
            return;
        }
        SharedPreferences pref = context.getSharedPreferences(name, 0);
        pref.edit().clear().apply();

        list.remove(name);
        setProfilesList(list);

        List<String> free = getProfileListEx("free");
        free.add(name);
        setProfileListEx("free", free);
    }
}
