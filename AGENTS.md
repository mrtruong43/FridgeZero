# FridgeZero Workspace Rules

## Build & Cache Environment
- Always ensure `GRADLE_USER_HOME` points to `E:\.gradle` before running Gradle commands (`$env:GRADLE_USER_HOME = 'E:\.gradle'`) so build caches do not spill onto drive `C:`.

## Mandatory Post-Build Reporting & Source Summary
After **every** Android APK build (`assembleDebug`, `assembleRelease`, etc.):
1. Run `python scripts/post_build_report.py` from the repository root (`e:\AI agent\FridgeZero`).
   - This updates `source_summary.md` with the latest source code snapshot.
   - It copies the newly built debug APK to `FridgeZero-debug.apk` at the repository root for quick access.
   - It computes code change metrics (`git diff HEAD --numstat`) and the newly built APK size delta.
2. Include a **Post-Build Summary** section in the final response containing:
   - **Code changes:** Files modified/added and exact lines added/removed (`+X / -Y`).
   - **APK size change:** Exact size of the newly built APK and its delta (in bytes, KiB/MiB, and percentage) compared against the previous build and/or root `FridgeZero-debug.apk` baseline.
   - **Summary file update:** Confirmation that `source_summary.md` was updated with the latest source code.
