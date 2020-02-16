package org.mechdancer.symbol

fun main() {
    run {
        val x by variable
        val y by variable

        val e = 9 * x * x * y + 7 * x * x + 4 * y - 2
        val dx = d(e) / d(x)
        val dy = d(e) / d(y)
        val dxy = d(dx) / d(y)

        println(e)
        println(dx)
        println(dy)
        println(dxy)

        println(e.substitute { this[x] = 4; this[y] = 10 })
    }
    println()
    run {
        val x by variable
        val y by variable
        val e = sqrt((x pow 2) + (y pow 2))
        println(e)
        println(e d x)
    }
    println()
    run {
        val x by variable
        val e = ln(9 * x - 7)
        println(e)
        println(e d x)
    }
}
