package org.mechdancer.symbol.optimize

import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Exponential
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import kotlin.math.E

/** 变量 [v] 在 [[min], [max]] 区间的约束条件 */
data class Domain(val v: Variable, val min: Constant, val max: Constant) {
    private fun exp(e: Expression) = Exponential[Constant(E), e]

    /** 代入表达式向量，若变量不在目标区域则产生一个损失项 */
    fun check(p: ExpressionVector) =
        (p[v] as? Constant)?.let {
            when {
                it < min -> Triple(v, v * (it - min), it - min)
                it > max -> Triple(v, v * (it - max), it - max)
                else     -> null
            }
        }
}
