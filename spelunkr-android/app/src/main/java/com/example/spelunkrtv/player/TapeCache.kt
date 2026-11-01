package com.example.spelunkrtv.player

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSink
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.spelunkrtv.schedule.Program
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Keeps recently seen tapes' indexes on disk. An index (the MP4 "moov") has to be downloaded in
 * full before the first frame, and runs to 40 MB on the long tapes. Some tapes keep it at the
 * start of the file, some at the very end; either way only the index (and the file's first few
 * bytes) is ever written, so this holds a few dozen indexes, never hours of video. Coming back
 * to a channel, retrying, or reopening the app then skips the long download.
 */
@OptIn(UnstableApi::class)
object TapeCache {
    private const val MAX_BYTES = 300L * 1024 * 1024
    /** Atom headers next to the index (ftyp, free, uuid, the mdat header) come along too. */
    internal const val INDEX_MARGIN = 64L * 1024
    /** Where an index that sits at the end of its file starts, kept with the cached file. */
    private const val INDEX_AT = "spelunkr-index-at"

    @Volatile private var cache: SimpleCache? = null
    /** Cache key (the tape's archive.org URL) to its index size plus margin. */
    private val indexSpans = ConcurrentHashMap<String, Long>()

    /** One per process: SimpleCache refuses a second instance on the same folder. */
    fun get(context: Context): Cache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "tape-indexes"),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context.applicationContext),
        ).also { cache = it }
    }

    /** Remembers how big [program]'s index is, so the player may keep exactly that part of the file. */
    fun note(program: Program): Long? =
        program.indexBytes?.let { it + INDEX_MARGIN }?.also { indexSpans[program.videoUrl] = it }

    internal fun indexSpan(key: String): Long = indexSpans[key] ?: 0

    internal fun markIndexAt(cache: Cache, key: String, position: Long) {
        runCatching { cache.applyContentMetadataMutations(key, ContentMetadataMutations().set(INDEX_AT, position)) }
    }

    fun isIndexCached(context: Context, program: Program): Boolean {
        val size = program.indexBytes ?: return false
        note(program)
        val cache = get(context)
        val key = program.videoUrl
        // The index right after the file's opening atoms...
        if (cache.isCached(key, 0, size)) return true
        // ...or at the end of the file, where it was seen last time.
        val at = cache.getContentMetadata(key).get(INDEX_AT, -1L)
        return at > 0 && cache.isCached(key, at, size) && cache.isCached(key, 0, HEAD_BYTES)
    }

    /** Drops what is kept of [program]'s file, after it turned out wrong. */
    fun forget(context: Context, program: Program) {
        runCatching { get(context).removeResource(program.videoUrl) }
        Log.i(LivePlayer.TAG, "Dropped the saved index of \"${program.title}\"")
    }

    /** What the player reads through: the disk first, then the network, saving index bytes on the way. */
    fun playerDataSource(context: Context, upstream: DataSource.Factory): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory { IndexOnlySink(get(context)) }
            // A broken or full disk must never stop a tape: read around it.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setEventListener(CacheLog)

    /** The few bytes at the very start the player reads before jumping to an index at the end. */
    internal const val HEAD_BYTES = 16L

    /** Says in the log when the disk is skipped, and how much a tune read from it. */
    private object CacheLog : CacheDataSource.EventListener {
        override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
            if (cachedBytesRead > HEAD_BYTES * 4) Log.i(LivePlayer.TAG, "Read ${cachedBytesRead / 1000} KB of tape index from this device")
        }

        override fun onCacheIgnored(reason: Int) {
            Log.w(LivePlayer.TAG, "Skipped the tape cache (reason $reason)")
        }
    }
}

/**
 * Passes on only the bytes that belong to a tape's index, whether the index sits at the start
 * of the file or at its end; the hours of video in between are not kept.
 */
@OptIn(UnstableApi::class)
private class IndexOnlySink(private val cache: Cache) : DataSink {
    private val sink = CacheDataSink(cache, CacheDataSink.DEFAULT_FRAGMENT_SIZE)
    private var allowed = 0L
    private var open = false

    override fun open(dataSpec: DataSpec) {
        val key = dataSpec.key ?: dataSpec.uri.toString()
        val span = TapeCache.indexSpan(key)
        val fileEnd = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.position + dataSpec.length else -1L
        allowed = when {
            span <= 0 -> 0
            // The start of the file: the opening atoms, and an index placed first.
            dataSpec.position < span -> span - dataSpec.position
            // The last stretch of the file: an index placed at the end.
            fileEnd > 0 && dataSpec.position >= fileEnd - span -> {
                TapeCache.markIndexAt(cache, key, dataSpec.position)
                fileEnd - dataSpec.position
            }
            else -> 0
        }
        open = allowed > 0
        if (open) sink.open(dataSpec)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (!open || allowed <= 0) return
        val n = minOf(length.toLong(), allowed).toInt()
        sink.write(buffer, offset, n)
        allowed -= n
    }

    override fun close() {
        if (open) sink.close()
        open = false
    }
}

/**
 * Fetches tape indexes into [TapeCache] ahead of need, like the site's neighbour warming: the
 * next tape on the channel, and what is on the channels either side. One at a time, in the
 * background, and only while the tape on screen has plenty buffered. It reads the file's atom
 * headers to find where the index is, then fetches exactly that.
 */
@OptIn(UnstableApi::class)
class TapeWarmer(private val context: Context, upstream: DataSource.Factory) {
    private val factory = CacheDataSource.Factory()
        .setCache(TapeCache.get(context))
        .setUpstreamDataSourceFactory(upstream)
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "spelunkr-warm").apply { isDaemon = true } }
    @Volatile private var wanted: List<Program> = emptyList()
    @Volatile private var writer: CacheWriter? = null
    @Volatile private var writingKey: String? = null
    @Volatile private var running = false
    /** Tapes whose index could not be fetched lately, so a dead link is not retried every second. */
    private val failedAt = ConcurrentHashMap<String, Long>()

    /** Asks for these indexes, most wanted first; anything already cached is skipped. */
    fun warm(programs: List<Program>) {
        val now = SystemClock.elapsedRealtime()
        val list = programs
            .filter { it.indexBytes != null }
            .filter { failedAt[it.videoUrl]?.let { at -> now - at > RETRY_FAILED_MS } ?: true }
            .filter { !TapeCache.isIndexCached(context, it) }
            .distinctBy { it.videoUrl }
        wanted = list
        writingKey?.let { key -> if (list.none { it.videoUrl == key }) writer?.cancel() }
        if (list.isNotEmpty() && !running) {
            running = true
            executor.execute(::drain)
        }
    }

    /** Stops warming: a tune is starting and needs the whole line. */
    fun stop() {
        wanted = emptyList()
        writer?.cancel()
    }

    private fun drain() {
        try {
            while (true) {
                val next = wanted.firstOrNull { !TapeCache.isIndexCached(context, it) } ?: return
                writingKey = next.videoUrl
                val started = SystemClock.elapsedRealtime()
                try {
                    val (at, size) = findIndex(next.videoUrl) ?: error("no index found in the first atoms")
                    // A tune may have started meanwhile: never download the same bytes as the player.
                    if (wanted.none { it.videoUrl == next.videoUrl }) continue
                    // An index at the start is fetched with everything before it.
                    val from = if (at < TapeCache.INDEX_MARGIN) 0 else at
                    if (from > 0) TapeCache.markIndexAt(TapeCache.get(context), next.videoUrl, at)
                    val spec = DataSpec.Builder().setUri(next.videoUrl).setPosition(from).setLength(at + size - from).build()
                    val w = CacheWriter(factory.createDataSourceForDownloading(), spec, null, null)
                    writer = w
                    w.cache()
                    Log.i(LivePlayer.TAG, "Warmed \"${next.title}\": ${size / 1000} KB index " +
                        "(${if (from > 0) "at the end" else "at the start"}) in ${SystemClock.elapsedRealtime() - started} ms")
                } catch (e: Exception) {
                    // A cancel is not a failure: the tape just isn't wanted any more.
                    if (wanted.any { it.videoUrl == next.videoUrl }) {
                        failedAt[next.videoUrl] = SystemClock.elapsedRealtime()
                        Log.w(LivePlayer.TAG, "Couldn't warm \"${next.title}\": $e")
                    }
                }
                wanted = wanted.filter { it.videoUrl != next.videoUrl }
            }
        } finally {
            writer = null
            writingKey = null
            running = false
        }
    }

    /** Walks the file's top-level atoms (reading only their headers) to the moov: where it starts, and its size. */
    private fun findIndex(url: String): Pair<Long, Long>? {
        var position = 0L
        repeat(MAX_ATOMS) {
            val header = read(url, position, 16)
            if (header.size < 8) return null
            var size = u32(header, 0)
            val type = String(header, 4, 4, Charsets.ISO_8859_1)
            if (size == 1L && header.size >= 16) size = u64(header, 8)
            if (type == "moov") return position to size
            if (size < 8) return null
            position += size
        }
        return null
    }

    /** Reads [length] bytes at [position] through the cache, so they are kept for the player too. */
    private fun read(url: String, position: Long, length: Int): ByteArray {
        val source = factory.createDataSource()
        try {
            source.open(DataSpec.Builder().setUri(url).setPosition(position).setLength(length.toLong()).build())
            val out = ByteArray(length)
            var got = 0
            while (got < length) {
                val n = source.read(out, got, length - got)
                if (n == C.RESULT_END_OF_INPUT) break
                got += n
            }
            return out.copyOf(got)
        } finally {
            source.close()
        }
    }

    private fun u32(b: ByteArray, at: Int): Long =
        (0 until 4).fold(0L) { acc, i -> (acc shl 8) or (b[at + i].toLong() and 0xFF) }

    private fun u64(b: ByteArray, at: Int): Long =
        (0 until 8).fold(0L) { acc, i -> (acc shl 8) or (b[at + i].toLong() and 0xFF) }

    private companion object {
        const val RETRY_FAILED_MS = 5 * 60_000L
        const val MAX_ATOMS = 8
    }
}
