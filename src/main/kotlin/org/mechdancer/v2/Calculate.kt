package org.mechdancer.v2

fun Sequence<Expression>.sum() = Sum.sum(this)
fun Iterable<Expression>.sum() = Sum.sum(this)
fun Array<Expression>.sum() = Sum.sum(*this)

fun Sequence<Expression>.product() = Product.product(this)
fun Iterable<Expression>.product() = Product.product(this)
fun Array<Expression>.product() = Product.product(*this)

fun <T> Sequence<T>.sumBy(block: (T) -> Expression) = Sum.sum(map(block))
fun <T> Iterable<T>.sumBy(block: (T) -> Expression) = Sum.sum(map(block))
fun <T> Array<T>.sumBy(block: (T) -> Expression) = Sum.sum(map(block))

fun <T> Sequence<T>.productBy(block: (T) -> Expression) = Product.product(map(block))
fun <T> Iterable<T>.productBy(block: (T) -> Expression) = Product.product(map(block))
fun <T> Array<T>.productBy(block: (T) -> Expression) = Product.product(map(block))
