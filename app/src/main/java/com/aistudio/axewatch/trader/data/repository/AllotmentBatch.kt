package com.aistudio.axewatch.trader.data.repository

import kotlinx.coroutines.CancellationException

/** One failed applicant must not prevent later applicants from being checked. */
internal suspend fun <P, R> runAllotmentBatch(
    applicants: List<P>,
    onProgress: (Int, Int) -> Unit,
    check: suspend (P) -> R,
    onFailure: suspend (P) -> R
): List<R> {
    onProgress(0, applicants.size)
    return applicants.mapIndexed { index, applicant ->
        val result = try { check(applicant) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { onFailure(applicant) }
        onProgress(index + 1, applicants.size)
        result
    }
}
