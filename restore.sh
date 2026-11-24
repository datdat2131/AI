#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
PROJECT="$ROOT/AstraxAI"
rm -rf "$PROJECT"
mkdir -p "$PROJECT/src/main/java/com/astrax/ai/util" \
         "$PROJECT/src/main/java/com/astrax/ai/admin" \
         "$PROJECT/src/main/java/com/astrax/ai/gui" \
         "$PROJECT/src/main/java/com/astrax/ai/api" \
         "$PROJECT/src/main/java/com/astrax/ai/chat" \
         "$PROJECT/src/main/java/com/astrax/ai/command" \
         "$PROJECT/src/main/java/com/astrax/ai/memory" \
         "$PROJECT/src/main/resources"
copy() { cp "$ROOT/$1" "$PROJECT/$2"; }
copy CooldownManager.java src/main/java/com/astrax/ai/util/CooldownManager.java
copy Sched.java src/main/java/com/astrax/ai/util/Sched.java
copy Sanitizer.java src/main/java/com/astrax/ai/util/Sanitizer.java
copy Settings.java src/main/java/com/astrax/ai/util/Settings.java
copy Text.java src/main/java/com/astrax/ai/util/Text.java
copy WarningTracker.java src/main/java/com/astrax/ai/admin/WarningTracker.java
copy AdminService.java src/main/java/com/astrax/ai/admin/AdminService.java
copy ServerMonitor.java src/main/java/com/astrax/ai/admin/ServerMonitor.java
copy GuiListener.java src/main/java/com/astrax/ai/gui/GuiListener.java
copy MainMenu.java src/main/java/com/astrax/ai/gui/MainMenu.java
copy AstraxAI.java src/main/java/com/astrax/ai/AstraxAI.java
copy AiClient.java src/main/java/com/astrax/ai/api/AiClient.java
copy ChatMessage.java src/main/java/com/astrax/ai/api/ChatMessage.java
copy AiResult.java src/main/java/com/astrax/ai/api/AiResult.java
copy ChatListener.java src/main/java/com/astrax/ai/chat/ChatListener.java
copy ChatService.java src/main/java/com/astrax/ai/chat/ChatService.java
copy ChatInputManager.java src/main/java/com/astrax/ai/chat/ChatInputManager.java
copy KnowledgeBase.java src/main/java/com/astrax/ai/chat/KnowledgeBase.java
copy AiCommand.java src/main/java/com/astrax/ai/command/AiCommand.java
copy AdminCommand.java src/main/java/com/astrax/ai/command/AdminCommand.java
copy ChatMemory.java src/main/java/com/astrax/ai/memory/ChatMemory.java
copy knowledge.yml src/main/resources/knowledge.yml
copy plugin.yml src/main/resources/plugin.yml
copy config.yml src/main/resources/config.yml
copy README.md README.md
copy pom.xml pom.xml
copy .gitignore .gitignore
echo "Restored Maven project at $PROJECT"
