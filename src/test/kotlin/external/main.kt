package external

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.function.vector.normalize
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.listVectorOfZero
import org.mechdancer.algebra.implement.vector.toListVector
import org.mechdancer.algebra.implement.vector.vector2D
import org.mechdancer.remote.presets.RemoteHub
import org.mechdancer.remote.presets.remoteHub
import org.mechdancer.symbol.networksInfo
import org.mechdancer.symbol.paint
import org.mechdancer.symbol.paintFrame2
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** 信号定义 */
class Signal(val t0: Int = 0, val values: Vector) {
    val length get() = values.dim

    fun delay(t: Int) = Signal(t0 + t, values)

    operator fun get(t: Int) =
        (t - t0).takeIf { it in 0 until values.dim }?.let(values::get) ?: .0

    operator fun times(k: Number) = Signal(t0, values * k)
    operator fun plus(others: Signal): Signal {
        val begin = min(t0, others.t0)
        val end = max(t0 + length, others.t0 + others.length)
        return Signal(begin, List(end - begin) { i ->
            this[begin + i] + others[begin + i]
        }.toListVector())
    }

    fun noise(snr: Double): Signal {
        val p = values.length / snr
        val engine = java.util.Random()
        val n = List(length) { engine.nextGaussian() }.toListVector().normalize() * p
        return Signal(t0, values + n)
    }

    infix fun dot(others: Signal): Double {
        val begin = min(t0, others.t0)
        val end = max(t0 + length, others.t0 + others.length)
        return (begin until end).sumByDouble { this[it] * others[it] }
    }

    infix fun xcorr(others: Signal): Signal {
        val begin = t0 - others.length + 1
        val end = t0 + length + others.length - 2
        return Signal(begin, List(end - begin) {
            this dot others.delay(1 - others.length + it)
        }.toListVector())
    }

    fun filter(block: (Double) -> Double) =
        Signal(t0, values.toList().map(block).toListVector())
}

fun main() {
    val remote = remoteHub("定位优化").apply {
        openAllNetworks()
        println(networksInfo())
    }

    val signal = Signal(values = (.0..2 * PI).sample(.1, ::sin))
    val delay = 50

    while (true) {
        val environment = List(delay) { i ->
            (1 - i / delay.toDouble()) * Random.nextDouble() / (delay * .25)
        }
        var output = Signal(values = listVectorOfZero(1))
        environment.forEachIndexed { i, k ->
            output += signal.delay(i) * k
        }
        var memory = .0
        output = output.noise(.5)
//            .filter {
//            memory = it * .1 + memory * .9
//            memory
//        }
        remote.paint("发射", signal)
        remote.paint("接收", output)
        remote.paint("自相关", (signal xcorr signal) * .01)
        remote.paint("互相关", (output xcorr signal) * .01)
        Thread.sleep(1000)
    }
}

// 采样
private fun ClosedFloatingPointRange<Double>.sample(
    step: Double,
    f: (Double) -> Double
) =
    sequence {
        var t = start
        while (t < endInclusive) {
            yield(f(t))
            t += step
        }
    }.toList().toListVector()

// 画信号
private fun RemoteHub.paint(topic: String, signal: Signal) =
    signal.values.toList()
        .mapIndexed { i, value -> vector2D(signal.t0 + i, value) }
        .let { paintFrame2(topic, listOf(it)) }
