package org.mechdancer.symbol

import org.mechdancer.symbol.linear.Field
import org.mechdancer.symbol.linear.VariableSpace

/** 梯度下降步骤函数 := 当前位置 -> 实际步长 */
typealias GradientDescentStep = (Field) -> Pair<Field, Double>

fun gradientDescent(
    error: Expression,
    space: VariableSpace,
    pid: PIDLimiter
): GradientDescentStep {
    val gradField = space.hamiltonian * error
    return { p ->
        val grad = gradField.substitute(p)
        val l = (grad.length as Constant).value
        val k = pid(l)
        p - grad * (k / l) to k
    }
}

/** 递推计算 */
fun <T> recurrence(
    init: T, block: (T) -> T
) = sequence {
    var t = init
    while (true) {
        t = block(t)
        yield(t)
    }
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
        val r = ka * (e + ki * sum + kd * dd)
        return when {
            r > +max -> +max
            r < -max -> -max
            else     -> r
        }
    }
}
