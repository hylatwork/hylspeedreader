package eu.hylatwork.hylspeedreader.ui

import androidx.compose.runtime.Composable
import eu.hylatwork.hylspeedreader.data.epub.DocumentHandle
import eu.hylatwork.hylspeedreader.data.epub.IosDocumentHandle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject

/**
 * iOS picker backed by `UIDocumentPickerViewController` (opening the EPUB
 * container type, copied into the app sandbox). Presented from the top-most
 * view controller of the key window.
 */
@Composable
actual fun rememberEpubPicker(onResult: (DocumentHandle?) -> Unit): () -> Unit = {
    val epubType = UTType.UTTypeWithIdentifier("org.idpf.epub-container")
    val picker = UIDocumentPickerViewController(
        forOpeningContentTypes = listOfNotNull(epubType),
        asCopy = true
    )
    picker.delegate = EpubMediaPickerDelegate(onResult)
    topViewController()?.presentViewController(picker, animated = true, completion = null)
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

private class EpubMediaPickerDelegate(
    private val onResult: (DocumentHandle?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        onResult((didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.let(::IosDocumentHandle))
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}
