#!/bin/zsh
# 사용: tap.sh "버튼 텍스트"  (uiautomator로 텍스트를 찾아 가운데를 누른다)
ADB="$HOME/Library/Android/sdk/platform-tools/adb -s emulator-5554"
eval $ADB shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
XML=$(eval $ADB shell cat /sdcard/ui.xml)
B=$(echo "$XML" | python3 -c "
import sys,re
x=sys.stdin.read(); t=sys.argv[1]
for m in re.finditer(r'<node [^>]*>', x):
    n=m.group(0)
    tx=re.search(r' text=\"([^\"]*)\"',n).group(1)
    if tx==t or (t.startswith('~') and t[1:] in tx):
        b=re.search(r'bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"',n).groups(); print(*b); break
" "$1")
[ -z "$B" ] && { echo "NOT FOUND: $1"; exit 1; }
set -- $(echo $B); X=$(( ($1+$3)/2 )); Y=$(( ($2+$4)/2 ))
eval $ADB shell input tap $X $Y; echo "tap $X,$Y"
