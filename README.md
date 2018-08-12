# Chatter
Chat app for gitter

<a href="https://play.google.com/store/apps/details?id=com.codingblocks.chatter">
  <img alt="Android app on Google Play"
       src="https://developer.android.com/images/brand/en_app_rgb_wo_45.png" />
</a>

[![CircleCI](https://img.shields.io/circleci/project/github/coding-blocks/Chatter.svg)](https://circleci.com/gh/coding-blocks/Chatter)
[![Build Status](https://travis-ci.org/coding-blocks/Chatter.svg?branch=development)](https://travis-ci.org/coding-blocks/Chatter)

Screenshots :

<img src="https://user-images.githubusercontent.com/29139786/43609356-62d17ee2-96c1-11e8-9414-d3a00490d362.gif" width=300>

<img src="https://user-images.githubusercontent.com/31950172/40463974-eed8a668-5f35-11e8-85e0-1675e4b3e457.png" width=300><img src="https://user-images.githubusercontent.com/31950172/40463975-ef141fc2-5f35-11e8-883c-f3758db0b350.png" width=300>

<img src="https://user-images.githubusercontent.com/31950172/40463976-ef50f79e-5f35-11e8-9ded-d08724292093.png" width=300><img src="https://user-images.githubusercontent.com/31950172/40463977-ef90a52e-5f35-11e8-80ed-910a00414458.png" width=300>

<img src="https://user-images.githubusercontent.com/31950172/40463978-efc9de48-5f35-11e8-90c7-6b6f3bb20894.png" width=300><img src="https://user-images.githubusercontent.com/31950172/40463979-f01303b6-5f35-11e8-90a8-2e38f8e41e47.png" width=300>

 ### Automating Publishing to the Play Store
 
    -The first APK or App Bundle needs to be uploaded via the Google Play Console because registering the app with the Play Store cannot be done using the Play Developer API.
    -To use this plugin, you must create a service account with access to the Play Developer API. Once that's done, you'll need to grant the following permissions to your service account for this plugin to work (go to Settings -> Developer account -> API access -> Service Accounts).
    -Once done download your PKCS12 key or json key somewhere and the location of key in the build.gradle file in the play block
    -Then run one of the following commands:
    | Command | Description |
   | ------------- | ------------- |
   | 'publishApkRelease'| Uploads the APK and the summary of recent changes. |
   | 'publishListingRelease'| Uploads the descriptions and images for the Play Store listing.|
   | 'publishRelease'| Uploads everything.|
   | 'bootstrapReleasePlayResources'| Fetch data from the Play Store & bootstrap the required files/folders.|
                                 
You can now type the following gradle commands such as the following:
 bash
./gradlew publishApkRelease

