package com.qoody.shared.data

import com.qoody.shared.domain.model.NewUnparsedCapture
import com.qoody.shared.domain.model.UnparsedCapture
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import com.qoody.shared.domain.repository.UnparsedCaptureRepository.Companion.MAX_ENTRIES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** [UnparsedCaptureRepository] kept in memory, for tests and screenshot fixtures. */
class InMemoryUnparsedCaptureRepository(
    seed: List<UnparsedCapture> = emptyList(),
) : UnparsedCaptureRepository {
    private val stored = MutableStateFlow(seed.map { Stored(it, dedupeKey = "seed-${it.id}") })
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1

    override val captures: Flow<List<UnparsedCapture>> =
        stored.map { list -> list.map { it.capture }.sortedByDescending { it.postedAt } }

    override suspend fun add(capture: NewUnparsedCapture) {
        stored.update { list ->
            if (list.any { it.dedupeKey == capture.dedupeKey }) return@update list
            val entry =
                UnparsedCapture(
                    id = nextId++,
                    packageName = capture.packageName,
                    appName = capture.appName,
                    title = capture.title,
                    text = capture.text,
                    postedAt = capture.postedAt,
                    reason = capture.reason,
                )
            (list + Stored(entry, capture.dedupeKey))
                .sortedByDescending { it.capture.postedAt }
                .take(MAX_ENTRIES)
        }
    }

    override suspend fun dismiss(id: Long) = stored.update { list -> list.filterNot { it.capture.id == id } }

    override suspend fun clear() = stored.update { emptyList() }

    private data class Stored(
        val capture: UnparsedCapture,
        val dedupeKey: String,
    )
}
