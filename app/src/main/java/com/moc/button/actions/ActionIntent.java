package com.moc.button.actions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

public class ActionIntent {
    static private Object parseValue(String str) {
        if (str.equals("")) {
            return str;
        }
        char c = str.charAt(0);
        if ((c == '"' || c == '\'')
                && str.endsWith(String.valueOf(c))) {
            return str.substring(1, str.length() - 1);
        }
        else if (str.equals("true")) {
            return true;
        }
        else if (str.equals("false")) {
            return false;
        }
        else try {
            c = str.charAt(str.length() - 1);
            if (c == 'l' || c == 'L') {
                return Long.parseLong(str.substring(0, str.length() - 1));
            }
            else if (c == 'f' || c == 'F') {
                return Float.parseFloat(str.substring(0, str.length() - 1));
            }
            else if (str.contains(".")) {
                return Double.parseDouble(str);
            }
            else {
                return Integer.parseInt(str);
            }
        } catch (NumberFormatException e) {
            Log.d("ActionIntent", "is not number: " + str);
            return str;
        }
    }

    static private void putIntentExtra(String string, Intent intent) {
        if (!string.contains(":")) {
            return;
        }
        String[] a = string.split(":", 2);
        String name = a[0], str = a[1];

        if (str.startsWith("{") && str.endsWith("}")) {
            String[] array = str.substring(1, str.length() - 1).split(",");

            if (array.length > 0) {
                Object[] objArray = new Object[array.length];

                boolean eqclass = true;
                Object object = parseValue(array[0]);
                Class<?> c = object.getClass();

                for (int i = 0; i < array.length; i++) {
                    object = parseValue(array[i]);
                    if (eqclass && object.getClass() != c) {
                        eqclass = false;
                    }
                    objArray[i] = object;
                }
                if (!eqclass || c == String.class) {
                    intent.putExtra(name,
                            Arrays.copyOf(objArray, objArray.length, String[].class));
                }
                else if (c == Boolean.class) {
                    boolean[] extra = new boolean[objArray.length];
                    for (int q = 0; q < objArray.length; q++) {
                        extra[q] = (boolean) objArray[q];
                    }
                    intent.putExtra(name, extra);
                }
                else if (c == Integer.class) {
                    int[] extra = new int[objArray.length];
                    for (int q = 0; q < objArray.length; q++) {
                        extra[q] = (int) objArray[q];
                    }
                    intent.putExtra(name, extra);
                }
                else if (c == Long.class) {
                    long[] extra = new long[objArray.length];
                    for (int q = 0; q < objArray.length; q++) {
                        extra[q] = (long) objArray[q];
                    }
                    intent.putExtra(name, extra);
                }
                else if (c == Float.class) {
                    float[] extra = new float[objArray.length];
                    for (int q = 0; q < objArray.length; q++) {
                        extra[q] = (float) objArray[q];
                    }
                    intent.putExtra(name, extra);
                }
                else if (c == Double.class) {
                    double[] extra = new double[objArray.length];
                    for (int q = 0; q < objArray.length; q++) {
                        extra[q] = (double) objArray[q];
                    }
                    intent.putExtra(name, extra);
                }
            }
        }
        else {
            Object object = parseValue(str);

            if (object instanceof Boolean) {
                intent.putExtra(name, (boolean) object);
            } else if (object instanceof Integer) {
                intent.putExtra(name, (int) object);
            } else if (object instanceof Long) {
                intent.putExtra(name, (long) object);
            } else if (object instanceof Float) {
                intent.putExtra(name, (float) object);
            } else if (object instanceof Double) {
                intent.putExtra(name, (double) object);
            } else if (object instanceof String) {
                intent.putExtra(name, (String) object);
            }
        }
    }

    static public void sendIntent(Context context, Map<String, String> params) {
        String str = params.get("param_intent_action");
        if (str == null || str.equals(""))
            return;
        Intent intent = new Intent(str);

        if ((str = params.get("param_intent_package")) != null
                && !str.equals("")) {
            if (str.contains("/")) {
                intent.setComponent(ComponentName.unflattenFromString(str));
            } else {
                intent.setPackage(str);
            }
        }
        if ((str = params.get("param_intent_data")) != null
                && !str.equals("")) {
            intent.setData(Uri.parse(str));
        }
        if ((str = params.get("param_intent_mimetype")) != null
                && !str.equals("")) {
            intent.setType(str);
        }
        if ((str = params.get("param_intent_category")) != null
                && !str.equals("")) {
            intent.addCategory(str);
        }
        if ((str = params.get("param_intent_extra")) != null
                && !str.equals("")) {

            String delimiter;
            if ((delimiter = params.get("param_intent_extra_delimiter")) != null
                    && !delimiter.equals("")) {
                String[] extras = str.split(Pattern.quote(delimiter));
                for (String extra : extras)
                    putIntentExtra(extra, intent);
            }
            else {
                putIntentExtra(str, intent);
            }
        }
        if ((str = params.get("param_intent_target")) != null
                && !str.equals("")) {
            switch (str) {
                case "activity":
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    break;
                case "broadcast":
                    context.sendBroadcast(intent);
                    break;
                case "service":
                    context.startService(intent);
                    break;
            }
        }
    }
}
