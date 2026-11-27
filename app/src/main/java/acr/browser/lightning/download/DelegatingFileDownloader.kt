package acr.browser.lightning.download

import javax.inject.Inject

/**
 * Selects which [FileDownloader] to perform the download.
 */
class DelegatingFileDownloader @Inject constructor(
    @Suppress("unused") private val dataImageFileDownloader: DataImageFileDownloader,
    @Suppress("unused") private val defaultFileDownloader: DefaultFileDownloader
) : FileDownloader {

    override suspend fun download(pendingDownload: PendingDownload) {
        // Скачивание полностью отключено
    }
}