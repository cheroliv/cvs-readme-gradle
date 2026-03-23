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

    private val plantUmlBlockRegex = Regex(
        """\[plantuml,\s*([^,\]\s]+)[^\]]*]\s*\n(-{4})\n(.*?)\n\2""",
        RegexOption.DOT_MATCHES_ALL
    )

    // Regex : capture href="README_plantuml{_xx}.adoc" dans les blocs passthrough ++++
    // Groupe 1 : tout ce qui précède le nom de fichier dans le href
    // Groupe 2 : le nom du fichier _plantuml source (ex: README_plantuml_fr.adoc)
    // Groupe 3 : tout ce qui suit dans la balise href
    private val langLinkRegex = Regex(
        """(href=")([^"]*README_plantuml(?:_[a-z]{2})?\.adoc)"""
    )

    @TaskAction
    fun process() {
        val root = sourceDir.get().asFile
        val sources = AdocSourceFile.scanDir(root)

        if (sources.isEmpty()) {
            logger.warn("Aucun fichier README_plantuml*.adoc trouvé dans : ${root.absolutePath}")
            return
        }

        sources.forEach { processSource(it) }
    }

    private fun processSource(src: AdocSourceFile) {
        val lang = src.effectiveLang(defaultLang.get())
        val content = src.file.readText()

        logger.lifecycle("╔═ Processing : ${src.file.name}  [lang=$lang]")

        val buildLangDir = File(buildImgDir.get().asFile, lang).also { it.mkdirs() }
        val repoLangDir = File(imgDir.get().asFile, lang).also { it.mkdirs() }

        var diagramCount = 0

        // ── Étape 1 : remplacement des blocs PlantUML ─────────────────────
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

            val relPath = "${imgDir.get().asFile.path}/${lang}/${name}.png"
                .removePrefix("${src.file.parentFile.path}/")
            "image::${relPath}[${name}]"
        }

        // ── Étape 2 : réécriture des liens inter-langues ───────────────────
        // README_plantuml.adoc     → href pointe vers README_plantuml_fr.adoc
        //                            devient → href="README_fr.adoc"
        // README_plantuml_fr.adoc  → href pointe vers README_plantuml.adoc
        //                            devient → href="README.adoc"
        val rewritten = langLinkRegex.replace(afterPlantuml) { match ->
            val prefix = match.groupValues[1]  // 'href="'
            val linkedSource = match.groupValues[2]  // ex: README_plantuml_fr.adoc

            // Convertit le nom source _plantuml en nom de fichier généré
            val generatedName = AdocSourceFile(File(linkedSource)).generatedFileName()

            logger.lifecycle("║  LINK : $linkedSource → $generatedName")
            "${prefix}${generatedName}"
        }

        // ── Étape 3 : écriture du fichier généré ──────────────────────────
        val generated = src.generatedFile()
        generated.writeText(rewritten)

        logger.lifecycle("║  OUT  : ${generated.name}  ($diagramCount diagram(s) replaced)")
        logger.lifecycle("╚═════════════════════════════════════════")
    }

    private fun generatePng(body: String, output: File) {
        val src = if (body.trim().startsWith("@startuml")) body
        else "@startuml\n$body\n@enduml"

        val reader = SourceStringReader(src)
        val fos = FileOutputStream(output)
        try {
            reader.outputImage(fos)
        } finally {
            fos.close()
        }
    }
}