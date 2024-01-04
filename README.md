The application allows you to handle volume button presses when the screen is off

Supported actions:
- Player control: play/pause, next/previous track, etc.
- Volume control
- Flashlight Enable/Disable
- Vibrate, Wakeup screen, etc.
- Send intent
- Switch handler

### About "Switch handler":
After this action the application will start to handle the buttons differently, according to the selected profile. If no action has been called for some time after switching the handler, the action defined in "Timeout" will be started.

This can be used to create different combinations of taps, for example:
- Long press:
    - Main handler:
        - Press Down: Switch handler to "Down handler"
    - Down handler:
        - Idle timeout: 250
        - Timeout action: Flashlight On/Off + Switch handler to "Main"
        - Release Down: Volume down + Switch handler to "Main"
- Double tap:
    - Main handler:
        - Press Down: Switch handler to "Down handler"
    - Down handler:
        - Idle timeout: 200
        - Timeout action: Volume down + Switch handler to "Main"
        - Press Down: Flashlight On/Off + Switch handler to "Main"
        
### About "Send Intent":
Values in extra are specified in the format `<name>:<value>`. You can write multiple values by separating them using the delimiter specified in "Extra delimiter". Example:
```
String:"One"
Boolean:true
Integer:42
Long:42L
Double:42.13
Float:42.13F
IntegerArray:{4,2,1,3}
StringArray:{"One","Three"}
```
