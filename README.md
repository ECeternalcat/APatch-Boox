<div align="center">
<a href="https://github.com/bmax121/APatch/releases/latest"><img src="https://images.weserv.nl/?url=https://raw.githubusercontent.com/bmax121/APatch/main/app/src/main/ic_launcher-playstore.png&mask=circle" style="width: 128px;" alt="logo"></a>

<h1 align="center">APatch Boox</h1>

[![Latest Release](https://img.shields.io/github/v/release/bmax121/APatch?label=Release&logo=github)](https://github.com/bmax121/APatch/releases/latest)
[![Nightly Release](https://img.shields.io/badge/Nightly%20release-gray?logo=hackthebox&logoColor=fff)](https://nightly.link/bmax121/APatch/workflows/build/main/APatch-Release)
[![Weblate](https://img.shields.io/badge/Localization-Weblate-teal?logo=weblate)](https://hosted.weblate.org/engage/APatch)
[![Channel](https://img.shields.io/badge/Follow-Telegram-blue.svg?logo=telegram)](https://t.me/APatchGroup)
[![GitHub License](https://img.shields.io/github/license/bmax121/APatch?logo=gnu)](/LICENSE)

</div>

> [!WARNING]
> **APatch Boox is an unofficial third-party build.** It is not released, signed, or supported by the official APatch project. Use it only if you understand the risks of modifying and running a root solution on your device.

## APatch Boox

This fork is intended for the **BOOX Tab Ultra C Pro** and similar BOOX firmware. The application ID is `me.bmax.apatch.boox` and the displayed name is `APatch Boox`, so it can be installed alongside the official APatch Manager.

### Why BOOX Needs This Build

BOOX uses a customized Android framework. On the Tab Ultra C Pro firmware tested with Android 13, kernel `4.14.190`, and firmware `GV2.027.SQ83A`, the official APatch Manager's Superuser page starts a temporary UID 0 RootService to enumerate packages. BOOX's customized `ActivityManagerService` can crash while handling that root process because its internal `ProcessRecord` is unavailable. The RootService exits immediately, leaving the Superuser page loading forever.

### What Is Changed

- The Superuser page enumerates installed packages through the Manager's `QUERY_ALL_PACKAGES` permission instead of starting that RootService.
- The application ID is changed to `me.bmax.apatch.boox` and the application name is changed to `APatch Boox`.
- A hard-coded official package data path used by the module helper is replaced with the current application package name.
- KernelPatch, APD, SuperKey handling, root authorization, and boot patching behavior are otherwise intended to remain unchanged.

### Important Notes

- This build does not make BOOX's modified framework official or generally compatible with every APatch release.
- The custom Manager APK must be paired with a boot image patched for the same APatch/KernelPatch installation. Installing this APK alone cannot provide root access.
- Back up the original boot image and important device data before changing boot or root components.
- The package name is different from the official Manager. Settings and Manager data are not automatically shared between the two applications.
- Report BOOX-specific issues to the maintainer of this build first. Do not assume that an issue is present in the official APatch project.

The patching of Android kernel and Android system.

- A new kernel-based root solution for Android devices.
- APM: Support for modules similar to Magisk.
- KPM: Support for modules that allow you to inject any code into the kernel (Provides kernel function `inline-hook` and `syscall-table-hook`).
- APatch relies on [KernelPatch](https://github.com/bmax121/KernelPatch/).
- The APatch UI and the APModule source code have been derived and modified from [KernelSU](https://github.com/tiann/KernelSU).

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/me.bmax.apatch/)

Or download the latest APK from the [Releases Section](https://github.com/bmax121/APatch/releases/latest).

## Supported Versions

- Only supports the ARM64 architecture.
- Only supports Android kernel versions 3.18 - 6.12

Support for Samsung devices with security protection: Planned

## Requirement

Kernel configs:

- `CONFIG_KALLSYMS=y` and `CONFIG_KALLSYMS_ALL=y`

- `CONFIG_KALLSYMS=y` and `CONFIG_KALLSYMS_ALL=n`: Initial support

## Security Alert

The **SuperKey** has higher privileges than root access.  
Weak or compromised keys can lead to unauthorized control of your device.  
It is critical to use robust keys and safeguard them from exposure to maintain the security of your device.

## Translation

To help translate APatch or improve existing translations, please use [Weblate](https://hosted.weblate.org/engage/apatch/). PR of APatch translation is no longer accepted, because it will conflict with Weblate.

<div align="center">

[![Translation Status](https://hosted.weblate.org/widget/APatch/open-graph.png)](https://hosted.weblate.org/engage/APatch/)

</div>

## Get Help

### Usage

For usage, please refer to [our official documentation](https://apatch.dev).  
It's worth noting that the documentation is currently not quite complete, and the content may change at any time.  
Furthermore, we need more volunteers to [contribute to the documentation](https://github.com/AndroidPatch/APatchDocs) in other languages.

### Updates

- Telegram Channel: [@APatchUpdates](https://t.me/APatchChannel)

### Discussions

- Telegram Group: [@APatchDiscussions(EN/CN)](https://t.me/Apatch_discuss)
- Telegram Group: [中文](https://t.me/APatch_CN_Group)

### More Information

- [Documents](docs/)

## Credits

- [KernelPatch](https://github.com/bmax121/KernelPatch/): The core.
- [Magisk](https://github.com/topjohnwu/Magisk): magiskpolicy.
- [KernelSU](https://github.com/tiann/KernelSU): App UI, and Magisk module like support.

## License

APatch is licensed under the GNU General Public License v3 [GPL-3](http://www.gnu.org/copyleft/gpl.html).
