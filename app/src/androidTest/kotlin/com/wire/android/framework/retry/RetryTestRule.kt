package com.wire.android.framework.retry

import android.util.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

const val RETRY_COUNT: Int = 3

class RetryTestRule(val retryCount: Int = RETRY_COUNT) : TestRule {

    private val TAG = RetryTestRule::class.java.simpleName

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            lateinit var caughtThrowable: Throwable

            // implement retry logic here
            for (i in 1..retryCount) {
                Log.i(TAG, "${description.displayName}: run #${i}")
                try {
                    base.evaluate()
                    //TODO if successful without a catch we need to intervent
                    // here and print out that the test run was successful after a failure
                    return
                } catch (t: Throwable) {
                    caughtThrowable = t
                    Log.e(TAG, "${description.displayName}: run #${i} failed")
                    //TODO i guess at this point we need to add the reporting modification
                }
            }

            Log.e(TAG, "${description.displayName}: giving up after $retryCount failures")
            throw caughtThrowable
        }
    }
}
