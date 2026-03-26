package readme

import net.sourceforge.plantuml.SourceStringReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.io.FileOutputStream

@CacheableTask
abstract class ProcessReadmeTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val imgDir: DirectoryProperty

    @get:OutputDirectory
    abstract val buildImgDir: DirectoryProperty

    @get:Input
    abstract val defaultLang: Property<String>

    // Matches any AsciiDoc block delimiter of 4+ identical chars: ----, ````, ......
    // Capture group 2 ensures opening and closing delimiter match exactly.
    private val plantUmlBlockRegex = Regex(
        """\[plantuml,\s*([^,\]\s]+)[^\]]*]\s*\n(-{4,}|`{4,}|\.{4,})\n(.*?)\n\2""",
        RegexOption.DOT_MATCHES_ALL
    )

    // Rewrites inter-language links in all three AsciiDoc forms:
    //   href="README_truth_fr.adoc"    → href="README_fr.adoc"
    //   link:README_truth_fr.adoc[...] → link:README_fr.adoc[...]
    //   xref:README_truth_fr.adoc[...] → xref:README_fr.adoc[...]
    private val langLinkRegex = Regex(
        """(href="|link:|xref:)([^"\[]*README_truth(?:_[a-z]{2})?\.adoc)"""
    )

    @TaskAction
    fun process() {
        val root    = sourceDir.get().asFile
        val sources = AdocSourceFile.scanDir(root)

        if (sources.isEmpty()) {
            logger.warn("No README_truth*.adoc file found in: ${root.absolutePath}")
            return
        }

        sources.forEach { processSource(it) }
    }

    private fun processSource(src: AdocSourceFile) {
        val lang    = src.effectiveLang(defaultLang.get())
        val content = src.file.readText()

        logger.lifecycle("╔═ Processing : ${src.file.name}  [lang=$lang]")

        val buildLangDir = File(buildImgDir.get().asFile, lang).also { it.mkdirs() }
        val repoLangDir  = File(imgDir.get().asFile,      lang).also { it.mkdirs() }

        var diagramCount = 0

        // ── Step 1: replace PlantUML blocks ───────────────────────────────────
        val afterPlantuml = plantUmlBlockRegex.replace(content) { match ->
            val name = match.groupValues[1].trim()
            val body = match.groupValues[3]
            diagramCount++

            val buildPng = File(buildLangDir, "${name}.png")
            generatePng(body, buildPng)
            logger.lifecycle("║  PNG  : ${buildPng.path}")

            val repoPng = File(repoLangDir, "${name}.png")
            buildPng.copyTo(repoPng, overwrite = true)
            logger.lifecycle("║  COPY : ${repoPng.path}")

            // Compute path relative to the source file using Path.relativize()
            // so it works correctly when the project is nested under the git root.
            val relPath = src.file.parentFile.toPath()
                .relativize(
                    File(imgDir.get().asFile, "$lang/${name}.png").toPath()
                )
                .toString()
                .replace('\\', '/')  // normalize separators on Windows

            "image::${relPath}[${name}]"
        }

        // ── Step 2: rewrite inter-language links ──────────────────────────────
        // README_truth.adoc    → link:README_truth_fr.adoc → link:README_fr.adoc
        // README_truth_fr.adoc → link:README_truth.adoc    → link:README.adoc
        val rewritten = langLinkRegex.replace(afterPlantuml) { match ->
            val prefix       = match.groupValues[1]
            val linkedSource = match.groupValues[2]

            val generatedName = AdocSourceFile(File(linkedSource)).generatedFileName()

            logger.lifecycle("║  LINK : $linkedSource → $generatedName")
            "${prefix}${generatedName}"
        }

        // ── Step 3: write generated file ──────────────────────────────────────
        val generated = src.generatedFile()
        generated.writeText(rewritten)

        logger.lifecycle("║  OUT  : ${generated.name}  ($diagramCount diagram(s) replaced)")
        logger.lifecycle("╚═════════════════════════════════════════")
    }

    private fun generatePng(body: String, output: File) {
        val src = if (body.trim().startsWith("@startuml")) body
        else "@startuml\n$body\n@enduml"

        val reader = SourceStringReader(src)
        FileOutputStream(output).use { fos ->
            reader.outputImage(fos)
        }
    }
}
