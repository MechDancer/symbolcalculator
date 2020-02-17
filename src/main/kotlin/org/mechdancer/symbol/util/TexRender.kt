package org.mechdancer.symbol.util

import org.mechdancer.symbol.Expression
import org.mechdancer.symbol.Tex
import java.io.File
import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
object TexRender {
    val baseDictionary = mk(File(System.getProperty("user.home"), ".symbol"))
    val dviDictionary = mk(File(baseDictionary, "dvi"))
    val svgDictionary = mk(File(baseDictionary, "svg"))

    fun render(expression: Expression) =
        File(dviDictionary, "${abs(expression.hashCode())}.tex")
            .apply { writeText(template(expression.toTex())) }
            .let(::dvi)
            .let(::svg)

    private fun mk(f: File) = f.apply { if (!exists()) mkdir() }

    private fun dvi(tex: File): File {
        Runtime.getRuntime().exec(
            arrayOf(
                "latex",
                "-interaction=batchmode",
                "-halt-on-error",
                "-output-directory=${dviDictionary.absolutePath}",
                tex.absolutePath)
        ).also { it.errorStream.bufferedReader().readText().takeIf(String::isNotBlank)?.run { println(this) } }
            .waitFor()
        return tex.absolutePath.replace("tex", "dvi").let(::File)
    }

    private fun svg(dvi: File): File {
        val new = File(svgDictionary, dvi.nameWithoutExtension + ".svg")
        Runtime.getRuntime().exec(
            arrayOf(
                "dvisvgm",
                dvi.absolutePath,
                "-n",
                "-v",
                "0",
                "-o",
                new.absolutePath)
        ).also { it.errorStream.bufferedReader().readText().takeIf(String::isNotBlank)?.run { println(this) } }
            .waitFor()
        return new
    }

    private fun template(tex: Tex): Tex = """
        \documentclass[preview]{standalone}

        \usepackage[english]{babel}
        \usepackage{amsmath}
        \usepackage{amssymb}
        \usepackage{dsfont}
        \usepackage{setspace}
        \usepackage{tipa}
        \usepackage{relsize}
        \usepackage{textcomp}
        \usepackage{mathrsfs}
        \usepackage{calligra}
        \usepackage{wasysym}
        \usepackage{ragged2e}
        \usepackage{physics}
        \usepackage{xcolor}
        \usepackage{microtype}
        \DisableLigatures{encoding = *, family = * }
        \linespread{1}

        \begin{document}

        $$
        $tex
        $$
        
        \end{document}

    """.trimIndent()
}
