# HylSpeedReader release (R8) rules.
#
# No custom keep rules are required: EPUB parsing uses only the platform XML DOM
# (javax.xml / org.w3c.dom), which R8 never removes, and there is no reflection
# or class-name lookup anywhere in the app. Compose/Material ship their own
# consumer rules. `./gradlew assembleRelease` completes R8 + resource shrinking
# with no missing-class warnings; add a scoped `-keep` only if a reflective
# feature is introduced later.