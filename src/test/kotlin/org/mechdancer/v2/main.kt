package org.mechdancer.v2

fun main() {
    val x by variable
    val y by variable

    println(x * (x + 1))

    val e = 9 * x * x * y + 7 * x * x + 4 * y - 2
    val dx = d(e) / d(x)
    val dy = d(e) / d(y)
    val dxy = d(dx) / d(y)

    println(e)
    println(dx)
    println(dy)
    println(dxy)

    println(e.substitute { this[x] = 1 })
}
