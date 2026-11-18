@echo off
cd /D "%~dp0"

cmd /D/C eolConverter crlf "**/.gitattributes"
cmd /D/C eolConverter crlf "**/.gitignore"
cmd /D/C eolConverter crlf "**/LICENSE*"
cmd /D/C eolConverter lf "**/*.sh"
cmd /D/C eolConverter crlf "**/*.md"
cmd /D/C eolConverter crlf "**/*.json"
cmd /D/C eolConverter crlf "**/*.yml"
cmd /D/C eolConverter crlf "**/*.yaml"
cmd /D/C eolConverter crlf ".github/workflows/*.yml"
cmd /D/C eolConverter crlf "**/*.xml"
cmd /D/C eolConverter crlf "**/*.svg"
cmd /D/C eolConverter crlf "**/*.pro"
cmd /D/C eolConverter crlf "**/*.kt"
cmd /D/C eolConverter crlf "**/*.kts"

cmd /D/C eolConverter crlf "**/*.swift"
cmd /D/C eolConverter crlf "**/*.xcconfig"
cmd /D/C eolConverter crlf "**/*.pbxproj"
cmd /D/C eolConverter crlf "**/*.xcworkspacedata"
cmd /D/C eolConverter crlf "**/*.xcscheme"

git add --all
git status

pause