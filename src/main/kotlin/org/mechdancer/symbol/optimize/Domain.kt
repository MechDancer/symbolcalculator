package org.mechdancer.symbol.optimize

import org.mechdancer.symbol.`^`
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Constant.Companion.e
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.minus
import org.mechdancer.symbol.plus

/** 变量 [v] 在 [[min], [max]] 区间的约束条件 */
data class Domain(val v: Variable, val min: Constant, val max: Constant) {
    private val function by lazy {
        when {
            min.value.isInfinite() -> e `^` v - max
            max.value.isInfinite() -> e `^` min - v
            else                   -> (e `^` min - v) + (e `^` v - max)
        }
    }

    /** 代入表达式向量，若变量不在目标区域则产生一个线性损失项 */
    fun mapLinear(p: ExpressionVector) =
        (p[v] as? Constant)?.let {
            when {
                it < min -> Triple(v, v * (it - min), it - min)
                it > max -> Triple(v, v * (it - max), it - max)
                else     -> null
            }
        }

    /** 代入表达式向量，若变量不在目标区域则产生一个指数损失项 */
    fun mapExp(p: ExpressionVector) =
        (p[v] as? Constant)?.let {
            when {
                it < min -> Triple(v, function, it - min)
                it > max -> Triple(v, function, it - max)
                else     -> null
            }
        }
}
