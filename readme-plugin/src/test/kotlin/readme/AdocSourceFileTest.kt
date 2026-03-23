package readme

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class AdocSourceFileTest {

    @Test
    fun `isSourceOfTruth accepte README_plantuml adoc`() {
        assertTrue(AdocSourceFile.isSourceOfTruth(File("README_plantuml.adoc")))
    }

    @Test
    fun `isSourceOfTruth accepte README_plantuml_fr adoc`() {
        assertTrue(AdocSourceFile.isSourceOfTruth(File("README_plantuml_fr.adoc")))
    }

    @Test
    fun `isSourceOfTruth rejette README adoc`() {
        assertFalse(AdocSourceFile.isSourceOfTruth(File("README.adoc")))
    }

    @Test
    fun `isSourceOfTruth rejette README_fr adoc`() {
        assertFalse(AdocSourceFile.isSourceOfTruth(File("README_fr.adoc")))
    }

    @Test
    fun `lang est null pour README_plantuml adoc`() {
        assertNull(AdocSourceFile(File("README_plantuml.adoc")).lang)
    }

    @Test
    fun `lang est fr pour README_plantuml_fr adoc`() {
        assertEquals("fr", AdocSourceFile(File("README_plantuml_fr.adoc")).lang)
    }

    @Test
    fun `generatedFileName pour README_plantuml adoc`() {
        assertEquals("README.adoc", AdocSourceFile(File("README_plantuml.adoc")).generatedFileName())
    }

    @Test
    fun `generatedFileName pour README_plantuml_fr adoc`() {
        assertEquals("README_fr.adoc", AdocSourceFile(File("README_plantuml_fr.adoc")).generatedFileName())
    }

    @Test
    fun `effectiveLang retourne la langue du fichier si presente`() {
        assertEquals("de", AdocSourceFile(File("README_plantuml_de.adoc")).effectiveLang("en"))
    }

    @Test
    fun `effectiveLang retourne le defaut si pas de langue dans le nom`() {
        assertEquals("en", AdocSourceFile(File("README_plantuml.adoc")).effectiveLang("en"))
    }
}
