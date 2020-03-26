package org.mechdancer.symbol.optimize

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.function.vector.times

/** 对向量进行增益 */
class GainMethod(
    override val dim: Int,
    private val k: Double
) : LinearController {
    override fun invoke(signal: Vector) =
        signal * k
}
