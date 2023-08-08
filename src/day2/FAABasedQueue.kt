package day2

import day1.*
import java.util.concurrent.atomic.*

// TODO: Copy the code from `FAABasedQueueSimplified`
// TODO: and implement the infinite array on a linked list
// TODO: of fixed-size `Segment`s.
class FAABasedQueue<E> : Queue<E> {
    private val head: AtomicReference<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicReference<Segment> // Tail pointer, similarly to the Michael-Scott queue
    private val enqIdx = AtomicLong(0L)
    private val deqIdx = AtomicLong(0L)

    init {
        val dummy = Segment(0L)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val curTail = tail.get()
            val i = enqIdx.getAndIncrement()
            val segment = findSegment(curTail, i / SEGMENT_SIZE)
            moveTailForward(segment)
            if (segment.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            // Is this queue empty?
            if (!shouldTryToDeque()) {
                return null
            }
            val curHead = head.get()
            val i = deqIdx.getAndIncrement()
            val segment = findSegment(curHead, i / SEGMENT_SIZE)
            moveHeadForward(segment)
            val idx = (i % SEGMENT_SIZE).toInt()
            if (segment.cells.compareAndSet(idx, null, POISONED)) {
                continue
            }
            return segment.cells.get(idx) as E?
        }
    }

    private fun shouldTryToDeque(): Boolean {
        while (true) {
            val curDeqIdx = deqIdx.get()
            val curEnqIdx = enqIdx.get()
            if (curDeqIdx != deqIdx.get()) {
                continue
            }
            return curDeqIdx <= curEnqIdx
        }
    }

    private fun findSegment(start: Segment, id: Long): Segment {
        var current = start
        while (current.id < id) {
            current.next.compareAndSet(null, Segment(current.id + 1))
            current = current.next.get()!!
        }
        return current
    }

    private fun moveTailForward(segment: Segment) {
        var curTail = tail.get()
        while (segment.id > curTail.id) {
            tail.compareAndSet(curTail, segment)
            curTail = tail.get()
        }
    }

    private fun moveHeadForward(segment: Segment) {
        var curHead = head.get()
        while (segment.id > curHead.id) {
            head.compareAndSet(curHead, segment)
            curHead = head.get()
        }
    }
}

private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
}

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2

private val POISONED = Any()
