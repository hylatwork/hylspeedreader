package eu.hylatwork.hylspeedreader

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Entry point consumed by `iosApp/iOSApp.swift`:
 * `MainViewControllerKt.MainViewController()`.
 */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
