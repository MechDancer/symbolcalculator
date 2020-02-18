package org.mechdancer.symbol

fun main() {
    run {
        val x by variable
        val y by variable
        val f = x + y + y
        println(f.d() / x.d())
    }
    run {
        val x by variable
        val y by variable

        val f = 9 * x * x * y + 7 * x * x + 4 * y - 2
        val dx = d(f) / d(x)
        val dy = d(f) / d(y)
        val dxy = d(dx) / d(y)

        println(f)
        println(dx)
        println(dy)
        println(dxy)
        println(d(f))

        println(f.substitute { this[x] = 4; this[y] = 10 })
    }
    println()
    run {
        val x by variable
        val y by variable
        val f = sqrt((x pow 2) + (y pow 2))
        println(f)
        println(d(f) / d(x))
    }
    println()
    run {
        val x by variable
        val f = ln(9 * x - 7)
        println(f)
        println(d(f) / d(x))
    }
    println()
    run {
        val x by variable
        val y by variable

        val f = x `^` 2
        println(f)
        println(f.substitute { this[x] = x * y })
    }
}
