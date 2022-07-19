# AirMessage for Android

![AirMessage's website header](README/hero.png)

AirMessage lets people use iMessage on the devices they like.
**AirMessage for Android** is the project's client for Android devices, with an emphasis on feeling like a part of the Android ecosystem.
AirMessage for Android can be downloaded from the [Google Play Store](https://play.google.com/store/apps/details?id=me.tagavari.airmessage).

Other AirMessage repositories:
[Server](https://github.com/airmessage/airmessage-server) |
[Web](https://github.com/airmessage/airmessage-web) |
[Connect (community)](https://github.com/airmessage/airmessage-connect-java)

## Getting started

AirMessage for Android uses [RxJava](https://github.com/ReactiveX/RxJava) to manage its internal logic.
If you're not familiar with RxJava, the repository's README file is a great place to get started:

[https://github.com/ReactiveX/RxJava](https://github.com/ReactiveX/RxJava#readme)

AirMessage for Android hooks in to Google Cloud and Firebase to utilize services like FCM, Google Maps, and Crashlytics.
The app will not build without a valid configuration, so to get started quickly, you can copy the `app/google-services.default.json` file to `app/google-services.json` to use a pre-configured Firebase project, or you may provide your own Firebase configuration file.
Similarly, the app will use the API key found in `secrets.default.properties` for Google Maps by default, and you can supply your own in `secrets.properties`.

## Building and running for AirMessage Connect

In order to help developers get started quickly, we host a separate open-source version of AirMessage Connect at `connect-open.airmessage.org`.
Firebase requires apps to be signed with a designated keystore file in order to enable Google-sign in, so you can copy the `signingshared/shared.keystore.disabled` file to `signingshared/shared.keystore`, which will cause Gradle to use this keystore file in debug builds instead of your local one.

Since this version of AirMessage Connect is hosted in a separate environment from official servers, you will have to be running a version of AirMessage Server that also connects to the same server.

We kindly ask that you do not use AirMessage's official Connect servers with any unofficial builds of AirMessage-compatible software.

---

Thank you for your interest in contributing to AirMessage!
You're helping to shape the future of an open, secure messaging market.
Should you have any questions, comments, or concerns, please shoot an email to [hello@airmessage.org](mailto:hello@airmessage.org).
