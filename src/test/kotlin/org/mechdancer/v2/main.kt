package org.mechdancer.v2

import org.mechdancer.v2.DExpression.Companion.d
import org.mechdancer.v2.DVariable.Companion.d

fun main() {
    val x by variable
    val y by variable

    val e = 9 * x * x * y + 7 * x * x + 4 * y - 2
    val dx = d(e) / d(x)
    val dy = d(e) / d(y)
    val d2 = d(dx) / d(y)

    println(e)
    println(dx)
    println(dy)
    println(d2)

    println(e.substitute { this[x] = 1 })
}
