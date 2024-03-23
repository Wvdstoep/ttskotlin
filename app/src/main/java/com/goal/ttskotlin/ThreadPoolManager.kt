package com.goal.ttskotlin

import android.os.Looper
import android.os.Process
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


class ThreadPoolManager private constructor() {
    companion object {
        val instance: ThreadPoolManager by lazy { Holder.INSTANCE }
    }

    private object Holder {
        val INSTANCE = ThreadPoolManager()
    }

    private var mExecutor: ThreadPoolExecutor? = null

    init {
        val corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1
        val namedThreadFactory: ThreadFactory = NamedThreadFactory("thread pool")
        mExecutor = ThreadPoolExecutor(
            corePoolSize,
            corePoolSize * 10,
            1L,
            TimeUnit.HOURS,
            LinkedBlockingQueue(),
            namedThreadFactory,
            ThreadPoolExecutor.DiscardPolicy()
        )
    }

    /**
     * 执行任务
     * @param runnable 需要执行的异步任务
     */
    fun execute(runnable: Runnable?) {
        if (runnable == null) {
            return
        }
        mExecutor!!.execute(runnable)
    }

    /**
     * single thread with name
     * @param name 线程名
     * @return 线程执行器
     */
    fun getSingleExecutor(name: String): ScheduledThreadPoolExecutor? {
        return getSingleExecutor(name, Thread.NORM_PRIORITY)
    }

    /**
     * single thread with name and priority
     * @param name thread name
     * @param priority thread priority
     * @return Thread Executor
     */
    fun getSingleExecutor(name: String, priority: Int): ScheduledThreadPoolExecutor? {
        return ScheduledThreadPoolExecutor(
            1,
            NamedThreadFactory(name, priority)
        )
    }

    /**
     * 从线程池中移除任务
     * @param runnable 需要移除的异步任务
     */
    fun remove(runnable: Runnable?) {
        if (runnable == null) {
            return
        }
        mExecutor!!.remove(runnable)
    }

    /**
     * 为线程池内的每个线程命名的工厂类
     */
    private class NamedThreadFactory private constructor(threadName: String, priority: Int) :
        ThreadFactory {
        private val group: ThreadGroup
        private val threadNumber = AtomicInteger(1)
        private val namePrefix: String
        private val priority: Int

        /**
         * Constructor
         * @param namePrefix 线程名前缀
         */
        private constructor(namePrefix: String) : this(namePrefix, Thread.NORM_PRIORITY)

        /**
         * Constructor
         * @param threadName 线程名前缀
         * @param priority 线程优先级
         */
        init {
            val s = System.getSecurityManager()
            group = if (s != null) s.threadGroup else Thread.currentThread().threadGroup
            namePrefix = threadName + "-" + POOL_NUMBER.getAndIncrement()
            this.priority = priority
        }

        override fun newThread(r: Runnable): Thread {
            val t = Thread(
                group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0
            )
            if (t.isDaemon) {
                t.isDaemon = false
            }
            t.priority = priority
            when (priority) {
                Thread.MIN_PRIORITY -> Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST)
                Thread.MAX_PRIORITY -> Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                else -> Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
            }
            return t
        }

        companion object {
            private val POOL_NUMBER = AtomicInteger(1)
        }
    }


    val isMainThread: Boolean
        /**
         * 判断当前线程是否为主线程
         * @return `true` if the current thread is main thread.
         */
        get() = Looper.myLooper() == Looper.getMainLooper()
}
class NamedThreadFactory(threadName: String, private val priority: Int) : ThreadFactory {
    private val group: ThreadGroup = System.getSecurityManager()?.threadGroup ?: Thread.currentThread().threadGroup!!
    private val threadNumber = AtomicInteger(1)
    private val namePrefix = "$threadName-${POOL_NUMBER.getAndIncrement()}"

    // Secondary constructor to set a default priority
    constructor(namePrefix: String) : this(namePrefix, Thread.NORM_PRIORITY)

    override fun newThread(r: Runnable): Thread {
        val t = Thread(group, r, "$namePrefix-${threadNumber.getAndIncrement()}", 0).apply {
            isDaemon = false
            priority = this@NamedThreadFactory.priority
        }
        when (priority) {
            Thread.MIN_PRIORITY -> Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST)
            Thread.MAX_PRIORITY -> Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            else -> Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
        }
        return t
    }

    companion object {
        private val POOL_NUMBER = AtomicInteger(1)
    }
}