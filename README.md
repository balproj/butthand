The application allows you to handle volume button presses when the screen is off

Possible actions:
- Player control: play/pause, next/previous track, etc.
- Volume control
- Flashlight Enable/Disable
- Vibrate, Wakeup screen, etc.
- Send intent
- Switch handler

Read more about the last item:
After this action the application will start to handle the buttons differently, according to the selected profile. If no action has been called for some time after switching the handler, the action defined in "Timeout" will be started.

This can be used to create different combinations of taps, for example:
- Long press:
    - Main handler:
        - Press Down: Swith handler to "Down handler"
    - Down handler:
        - Idle timeout: 250
        - Timeout action: Flashlighht On/Off + Swith handler to "Main"
        - Release Down: Volume down + Swith handler to "Main"
- Double tap:
    - Main handler:
        - Press Down: Swith handler to "Down handler"
    - Down handler:
        - Idle timeout: 200
        - Timeout action: Volume down + Swith handler to "Main"
        - Press Down: Flashlighht On/Off + Swith handler to "Main"
