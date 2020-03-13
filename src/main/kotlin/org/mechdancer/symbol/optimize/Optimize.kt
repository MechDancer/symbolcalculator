package org.mechdancer.symbol.optimize

import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Sum
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import kotlin.math.abs
import kotlin.math.sign

/** 构造取值范围结构 */
operator fun Variable.get(min: Constant, max: Constant) =
    Domain(this, min, max)

/** 优化步骤函数 := 当前位置 -> (新位置, 实际步长) */
typealias OptimizeStep<T> = (T) -> Pair<T, Double>

/** 使用牛顿迭代求关于变量 [v] 的一元函数 [f] 极小值 */
fun newton(
    f: Expression,
    v: Variable,
    positive: Boolean = false
): OptimizeStep<Double> {
    operator fun Expression.get(x: Double) =
        substitute(v, Constant(x)).toDouble()

    val df = f.d() / v.d()
    val ddf = df.d() / v.d()
    return { p ->
        val g = df[p]
        val l = abs(g / ddf[p])
        p - g.sign * l to l
    }
}

/** 使用牛顿法确定最优下降率 */
internal fun fastestWithNewton(
    e: Expression,
    p: ExpressionVector,
    dp: ExpressionVector
): Pair<ExpressionVector, Double> {
    val l by variable
    val next = p - dp.map { it * l }
    val a = optimize(1.0, 20, 1e-9, newton(e.substitute(next), l))
    return next.substitute(l, Constant(a)) to dp.length().toDouble() * abs(a)
}

/** 映射不等式约束并用牛顿法最速下降 */
internal fun Array<out Domain>.fastestOf(
    e: Expression,
    p: ExpressionVector,
    dp: ExpressionVector,
    which: Domain.(ExpressionVector) -> Triple<Variable, Expression, Constant>?
): Pair<ExpressionVector, Double> {
    val limit = mapNotNull { it.which(p) }
    return if (limit.isNotEmpty()) {
        val en = Sum[limit.map { (_, e, _) -> e } + e]
        val dn = limit.associate { (v, _, g) -> v to g }.let(::ExpressionVector)
        fastestWithNewton(en, p, dp + dn)
    } else
        fastestWithNewton(e, p, dp)
}

/** 优化计算 */
inline fun <T> optimize(
    init: T,
    maxTimes: Int,
    minStep: Double,
    block: OptimizeStep<T>
): T {
    var t = init
    repeat(maxTimes) {
        val (p, s) = block(t)
        if (s < minStep) return p
        t = p
    }
    return t
}

/** 递推计算 */
fun <T> recurrence(init: T, block: (T) -> T) =
    sequence {
        var t = init
        while (true) {
            t = block(t)
            yield(t)
        }
    }

/** 收敛或退出 */
inline fun <T : Any> Sequence<T>.firstOrLast(
    block: (T) -> Boolean
): T {
    var last: T? = null
    for (t in this) {
        if (block(t)) return t
        last = t
    }
    return last ?: throw NoSuchElementException("Sequence is empty.")
}
