package org.mechdancer.symbol

import org.mechdancer.symbol.linear.Field
import org.mechdancer.symbol.linear.VariableSpace

fun gradientDescent(
    error: Expression,
    space: VariableSpace,
    pid: PIDLimiter
) =
    sequence<(Field) -> Pair<Field, Double>> {
        val gradField = space.hamiltonian * error
        var i = 0
        while (true) yield { p ->
            val t0 = System.nanoTime()

            val grad = gradField.substitute(p)
            val l = (grad.length as Constant).value
            val k = pid(l)
            val step = grad * (k / l)
            val result = p - step

            val t = System.nanoTime() - t0

            println("第 ${++i} 次迭代（${t / 1E6}ms）")
            println("步长 = $k")
            println("损失 = ${error.substitute(p)}")
            println()
            result to k
        }
    }

fun <T, R> Sequence<T>.scan(
    init: R,
    block: (R, T) -> R
) = sequence {
    var r = init
    for (item in this@scan) yield(block(r, item).also { r = it })
}

class PIDLimiter(
    private val ka: Double,
    private val ki: Double,
    private val kd: Double,
    private val max: Double
) {
    private var last = .0
    private var sum = .0

    operator fun invoke(e: Double): Double {
        val dd = e - last
        last = e
        sum = .9 * sum + .1 * e
        val r = ka * e + ki * sum + kd * dd
        return when {
            r > +max -> +max
            r < -max -> -max
            else     -> r
        }
    }
}
