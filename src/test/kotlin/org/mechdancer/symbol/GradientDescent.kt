package org.mechdancer.symbol

import org.mechdancer.algebra.function.matrix.inverse
import org.mechdancer.algebra.function.matrix.times
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.Hamiltonian.Companion.dfToGrad
import org.mechdancer.symbol.linear.HessianMatrix
import org.mechdancer.symbol.linear.VariableSpace

/** 优化步骤函数 := 当前位置 -> (新位置, 实际步长) */
typealias OptimizeStep = (ExpressionVector) -> Pair<ExpressionVector, Double>

fun gradientDescent(
    error: Expression,
    space: VariableSpace,
    pid: PIDLimiter
): OptimizeStep {
    val gradField = space.hamiltonian * error
    return { p ->
        val grad = gradField.substitute(p)
        val l = grad.length().toDouble()
        val k = pid(l)
        p - grad * (k / l) to k
    }
}

fun newton(
    error: Expression,
    space: VariableSpace
): OptimizeStep {
    val df = error.d()
    val gradient = dfToGrad(df, space)
    val hessian = HessianMatrix(df.d(), space)
    val order = space.variables.toList()
    return { p ->
        val g = gradient.toVector(p, space)
        val h = hessian.toMatrix(p)
        val d = h.inverse() * g
        val step = order.mapIndexed { i, v -> v to Constant(d[i]) }.toMap().let(::ExpressionVector)
        p - step to d.length
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
            r > +max -> {
                clear()
                +max
            }
            r < -max -> {
                clear()
                -max
            }
            else     -> r
        }
    }

    private fun clear() {
        last = .0
        sum = .0
    }
}
