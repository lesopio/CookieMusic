package com.musicplayer.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongIdentityMatcherTest {
    private fun signature(
        size: Long = 10_000L,
        duration: Long = 180_000L,
        title: String = "Song",
        artist: String = "Artist"
    ) = SongIdentitySignature(size, duration, title, artist)

    @Test fun sameSourceMetadataWithinTwoSecondsMatches() {
        assertTrue(SongIdentityMatcher.isStrongMatch(signature(), signature(duration = 181_900L)))
    }

    @Test fun fourSecondDifferenceDoesNotMatch() {
        assertFalse(SongIdentityMatcher.isStrongMatch(signature(), signature(duration = 184_000L)))
    }

    @Test fun sameBasenameNeverOverridesDifferentMetadata() {
        assertFalse(SongIdentityMatcher.isStrongMatch(signature(title = "Album A track"), signature(title = "Album B track")))
    }

    @Test fun sameMetadataButDifferentFileSizeIsKeptSeparate() {
        assertFalse(SongIdentityMatcher.isStrongMatch(signature(), signature(size = 10_001L)))
    }

    @Test fun normalizedPunctuationStillMatches() {
        assertTrue(SongIdentityMatcher.isStrongMatch(signature(title = "《Song》"), signature(title = "song")))
    }
}
