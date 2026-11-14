#!/bin/zsh
# Pleos Connect 에뮬레이터를 켜고 부팅 완료를 기다린 뒤 Virtual Console 포트(8088)를 연결한다.
# 맥을 재부팅했거나 에뮬레이터 창을 닫은 뒤 다시 쓸 때 실행한다. 설치한 앱과 설정은 AVD 데이터 디스크에 남아 있다.
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
AVD=Pleos_Connect_v2.1.0
if ! "$ADB" devices | grep -q "^emulator-5554"; then
  nohup "$ANDROID_HOME/emulator/emulator" -avd "$AVD" -no-snapshot-load -no-boot-anim >"$HOME/.android/avd/pleos-emulator.log" 2>&1 &
  disown
fi
"$ADB" -s emulator-5554 wait-for-device
until [ "$("$ADB" -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
"$ADB" -s emulator-5554 forward tcp:8088 tcp:8088
echo "준비됨. Virtual Console: http://127.0.0.1:8088/"
echo "앱 다시 설치: ./gradlew :app:installDebug  /  실행: adb shell am start -n app.naerio/.MainActivity"
