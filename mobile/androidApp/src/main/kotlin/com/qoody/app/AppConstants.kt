package com.qoody.app

/** Links out of the app. */
object ProjectLinks {
    /** Public source repository. */
    const val SOURCE_CODE_URL = "https://github.com/theprinceraj/qoody"
}

/** Settings for the "Export ledger as CSV" file picker. */
object LedgerExport {
    const val MIME_TYPE = "text/csv"
    const val DEFAULT_FILE_NAME = "qoody-ledger.csv"
}

object BackupExport {
    const val MIME_TYPE = "application/octet-stream"
    const val DEFAULT_FILE_NAME = "qoody-backup.qoody"
}
