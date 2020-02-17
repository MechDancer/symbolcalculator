package org.mechdancer.symbol.util

import org.mechdancer.symbol.Expression
import org.mechdancer.symbol.Tex
import java.io.File
import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
object TexRender {
    val baseDictionary = File(System.getProperty("user.home"), ".symbol").apply {
        if (!exists())
            mkdir()
    }

    val dviDictionary = File(baseDictionary, "dvi").apply {
        if (!exists())
            mkdir()
    }
    val svgDictionary = File(baseDictionary, "svg").apply {
        if (!exists())
            mkdir()
    }

    private fun writeTexFile(expression: Expression): File {
        val tex = File(dviDictionary, "${abs(expression.hashCode())}.tex").apply {
            if (exists())
                delete()
            createNewFile()
        }
        tex.writeText(template(expression.toTex()))
        return tex
    }

    private fun dvi(tex: File): File {
        Runtime.getRuntime().exec(
            arrayOf(
                "latex",
                "-interaction=batchmode",
                "-halt-on-error",
                "-output-directory=${dviDictionary.absolutePath}",
                tex.absolutePath
            )
        ).also { println(it.errorStream.bufferedReader().readText()) }.waitFor()
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
                new.absolutePath
            )
        ).also { println(it.errorStream.bufferedReader().readText()) }.waitFor()
        return new
    }

    fun render(expression: Expression)=
        svg(dvi(writeTexFile(expression)))


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