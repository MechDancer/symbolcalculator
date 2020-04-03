package org.mechdancer.symbol

import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Constant.Companion.π
import kotlin.math.sqrt

fun main() {
    val x by variable
    val y by variable
    run {
        val f = sqrt(x * x + y) + 1 `^` 2

        val ddf = d(d(f))
        val dx = d(x)
        val dy = d(y)

        println("∂²f / ∂x²  = ${ddf / (dx * dx)}")
        println("∂²f / ∂x∂y = ${ddf / (dx * dy)}")
        println("∂²f / ∂y²  = ${ddf / (dy * dy)}")
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
    println()
    run {
        val f = Constant(1.0, -sqrt(3.0))
        println(f.toString())
        println(f.toStringAsComponent())
        println(f.toStringAsPolar())
    }
    println()
    run {
        val f = tan(x)
        println(f)
        println(f.substitute(x, π / 4))

        val df = d(f)
        println(df)
        println(df.substitute(x, π / 4))
    }
}
