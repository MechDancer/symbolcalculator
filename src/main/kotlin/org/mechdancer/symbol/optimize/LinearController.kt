package org.mechdancer.symbol.optimize

import org.mechdancer.algebra.core.Vector

/** 线性控制器 := (向量) -> 向量 */
interface LinearController {
    /** 输入输出向量必须有确定的维数，以便进行初始化 */
    val dim: Int

    /** 信号通过控制器 */
    operator fun invoke(signal: Vector): Vector
}
