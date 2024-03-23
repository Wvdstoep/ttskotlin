package com.goal.ttskotlin

import org.tensorflow.lite.Interpreter


/**
 * @author []" "Xuefeng Ding"">&quot;mailto:xuefeng.ding@outlook.com&quot; &quot;Xuefeng Ding&quot;
 * Created 2020-07-20 17:25
 */
abstract class AbstractModule {
    val option: Interpreter.Options
        get() {
            val options: Interpreter.Options = Interpreter.Options()
            options.setNumThreads(5)
            return options
        }
}

