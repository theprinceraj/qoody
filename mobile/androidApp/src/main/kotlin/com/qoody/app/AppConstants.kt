package com.qoody.app

/** Links out of the app. */
object ProjectLinks {
    /** Public source repository. TODO: replace with the real repository URL once it exists. */
    const val SOURCE_CODE_URL = "https://github.com"
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
