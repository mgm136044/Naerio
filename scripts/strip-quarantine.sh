#!/bin/zsh
# macOS 격리 표시(com.apple.quarantine) 제거: "실행할 수 없음" 경고 방지
for d in ~/.gradle ~/.android ~/Library/Android/sdk ~/AndroidStudioProjects; do
  [ -e "$d" ] && find "$d" -xattrname com.apple.quarantine -exec xattr -d com.apple.quarantine {} + 2>/dev/null
done
exit 0
