package com.paulmethfessel.bp.psiTests

import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.LocalTimeCounter
import java.io.File

abstract class PsiTest: LightPlatformTestCase() {
    fun getTestDataPath() = "src/test/testData"

    fun loadFile(name: String): PsiFile {
        val result = FileUtil.loadFile(File(getTestDataPath() + File.separatorChar + name))
        val content = StringUtil.convertLineSeparators(result)
        return PsiFileFactory.getInstance(project)
            .createFileFromText(name, JavaScriptFileType.INSTANCE, content, LocalTimeCounter.currentTime(), true, false)
    }
}
