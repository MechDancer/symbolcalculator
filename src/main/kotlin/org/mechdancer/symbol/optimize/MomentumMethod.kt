package org.mechdancer.symbol.optimize

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.listVectorOfZero

/** 动量法 := 增益 + 低通滤波 */
@Suppress("NonAsciiCharacters")
class MomentumMethod(
    dim: Int,
    private val η: Double,
    private val γ: Double
) : LinearController {
    private var memory = listVectorOfZero(dim)
    override val dim get() = memory.dim
    override fun invoke(signal: Vector) =
        (memory * γ + signal * η).also { memory = it }
}
