# Chatter
Chat app for gitter
This app provide chatting purpose for users.

<a href="https://play.google.com/store/apps/details?id=com.codingblocks.chatter">
  <img alt="Android app on Google Play"
       src="https://developer.android.com/images/brand/en_app_rgb_wo_45.png" />
</a>

[![CircleCI](https://img.shields.io/circleci/project/github/coding-blocks/Chatter.svg)](https://circleci.com/gh/coding-blocks/Chatter)
[![Build Status](https://travis-ci.org/coding-blocks/Chatter.svg?branch=development)](https://travis-ci.org/coding-blocks/Chatter)

Screenshots :

<img src="https://user-images.githubusercontent.com/29139786/43609356-62d17ee2-96c1-11e8-9414-d3a00490d362.gif" width=300>

<img src="https://user-images.githubusercontent.com/29139786/44072492-ca6a0e90-9fac-11e8-9f84-9ab288f4c33a.jpg" width=300><img src="https://user-images.githubusercontent.com/29139786/44072489-c9070904-9fac-11e8-9dad-527063fcde3c.jpg" width=300>

<img src="https://user-images.githubusercontent.com/29139786/44072488-c7e04950-9fac-11e8-9479-cfc132dc49a6.jpg" width=300><img src="https://user-images.githubusercontent.com/29139786/44072485-c6b03c70-9fac-11e8-85b4-2714f2cd8a18.jpg" width=300>

<img src="https://user-images.githubusercontent.com/29139786/44072581-4f6862cc-9fad-11e8-84d3-69d5f028afed.jpg" width=300><img src="https://user-images.githubusercontent.com/29139786/44072582-501f610c-9fad-11e8-9ab5-2e534b863b51.jpg" width=300>

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

