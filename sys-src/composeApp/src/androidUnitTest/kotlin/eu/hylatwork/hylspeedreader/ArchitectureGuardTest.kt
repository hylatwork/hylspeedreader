package eu.hylatwork.hylspeedreader

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Architecture guard test (Kotlin Multiplatform layout).
 *
 * Enforces the dependency laws documented in `sys-doc/ARCHITECTURE.md` §6 and
 * promised by `AGENTS.md` §3, scanning every KMP source set of `composeApp`:
 *  1. `domain/` classes have zero imports from `android.*`, `androidx.*` or any
 *     XML/HTML parsing library (`org.jsoup.*`, `javax.xml.*`, `org.w3c.dom.*`).
 *  2. `data/` classes have zero imports from `androidx.compose.*`,
 *     `androidx.lifecycle.ViewModel` or the `ui` layer.
 *  3. `ui/` Composables never recompute focus bucket indices or RSVP delays directly;
 *     those calculations originate from `RsvpWord` and `RsvpTiming` only.
 *  4. `ui/reader` + `ui/rsvp` Composables never import the `data` layer directly —
 *     platform services are wired in `ui/ReaderEntryPoint` only.
 *
 * Pure-JVM source scan (no Android/Compose test runtime required).
 */
class ArchitectureGuardTest {

    private val packageRoot = "eu/hylatwork/hylspeedreader"

    /** Kotlin source roots of the `composeApp` module across all targets. */
    private fun sourceRoots(): List<File> {
        val workingDir = System.getProperty("user.dir") ?: "."
        val moduleSrcCandidates = listOf("composeApp/src", "sys-src/composeApp/src")
        var dir: File? = File(workingDir).absoluteFile
        while (dir != null) {
            for (relative in moduleSrcCandidates) {
                val base = File(dir, relative)
                if (base.isDirectory) {
                    return listOf("commonMain", "androidMain", "iosMain")
                        .map { File(base, "$it/kotlin") }
                }
            }
            dir = dir.parentFile
        }
        error("Could not locate composeApp/src from $workingDir")
    }

    private fun kotlinFilesUnder(relative: String): List<File> =
        sourceRoots().flatMap { root ->
            File(root, relative)
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        }

    private fun importsOf(file: File): List<String> =
        file.readLines()
            .map { it.trim() }
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ").trim() }

    private fun assertNoImports(relative: String, forbiddenPrefixes: List<String>) {
        val violations = mutableListOf<String>()
        for (file in kotlinFilesUnder(relative)) {
            for (import in importsOf(file)) {
                if (forbiddenPrefixes.any { import.startsWith(it) }) {
                    violations += "${file.name}: $import"
                }
            }
        }
        assertTrue(
            "$relative must not import ${forbiddenPrefixes.joinToString()}, found: $violations",
            violations.isEmpty()
        )
    }

    @Test
    fun `domain layer has no android androidx or parser imports`() {
        assertNoImports(
            "$packageRoot/domain",
            listOf("android.", "androidx.", "org.jsoup", "javax.xml.", "org.w3c.dom.")
        )
    }

    @Test
    fun `data layer has no compose viewmodel or ui imports`() {
        assertNoImports(
            "$packageRoot/data",
            listOf("androidx.compose.", "androidx.lifecycle.ViewModel", "$packageRoot.ui.")
        )
    }

    @Test
    fun `ui composables do not recompute focus indices or rsvp delays`() {
        val forbiddenTokens = listOf(".focusIndexFor(", ".delayMs(")
        val composableRoots = listOf("$packageRoot/ui/reader", "$packageRoot/ui/rsvp")
        val violations = mutableListOf<String>()
        for (root in composableRoots) {
            for (file in kotlinFilesUnder(root)) {
                val content = file.readText()
                for (token in forbiddenTokens) {
                    if (content.contains(token)) violations += "${file.name}: $token"
                }
            }
        }
        assertTrue(
            "ui/ Composables must delegate focus/RSVP math to domain, found: $violations",
            violations.isEmpty()
        )
    }

    @Test
    fun `ui composables do not import the data layer`() {
        val composableRoots = listOf("$packageRoot/ui/reader", "$packageRoot/ui/rsvp")
        val violations = mutableListOf<String>()
        for (root in composableRoots) {
            for (file in kotlinFilesUnder(root)) {
                for (import in importsOf(file)) {
                    if (import.startsWith("$packageRoot.data.")) violations += "${file.name}: $import"
                }
            }
        }
        assertTrue(
            "ui/ Composables must reach the data layer only through ReaderViewModel, found: $violations",
            violations.isEmpty()
        )
    }
}
