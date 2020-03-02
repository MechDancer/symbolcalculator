package org.mechdancer.symbol

fun main() {
    val x by variable
    val y by variable
    run {
        val f = sqrt(x * x + y) + 1 `^` 2

        val ddf = d(d(f))
        val dx = d(x)
        val dy = d(y)

        println("∂2f / ∂x2  = ${ddf / (dx * dx)}")
        println("∂2f / ∂x∂y = ${ddf / (dx * dy)}")
        println("∂2f / ∂y2  = ${ddf / (dy * dy)}")
    }
    println()
    run {
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
        val f = ln(9 * x - 7)
        println(f)
        println(d(f) / d(x))
    }
    println()
    run {
        val f = x `^` 2
        println(f)
        println(f.substitute(x, 2))
        println(f.substitute { this[x] = x * y })
    }
}
