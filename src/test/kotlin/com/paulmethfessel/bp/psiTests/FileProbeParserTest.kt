package com.paulmethfessel.bp.psiTests

import com.intellij.psi.PsiFile
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FileProbeParserTest: PsiTest() {

    lateinit var file: PsiFile

    @Before
    fun setUpTests() {
        file = loadFile("test.js")
    }

    @Test
    fun testFindProbes() {
        file
    }
}
