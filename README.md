#youtube-dl Daemon

A quick and dirty migration of [youtube-download-bookmarklet](https://github.com/gugod/youtube-download-bookmarklet) to Groovy.

This is a daemon that allows you to dump audio from YouTube videos and save it in a file on disk.

## Installation
1. Make sure you have [youtube-dl](https://rg3.github.io/youtube-dl/) and [Groovy](http://www.groovy-lang.org/download.html) available in your search path
2. Run the deamon by executing the script `./youtube-dl_daemon.groovy` or add it as a startup script

## Usage
1. Navigate to http://localhost:3000/ and drag the link to your bookmarks toolbar
2. Click on the bookmark while on a YouTube tab to download the audio track from the video
The result will be shown in a popup window. Depending on your internet connection speed and the video size, it may take from a few seconds to minutes to show the result. The extracted audio will be downloaded to the `Downloads` folder in your home directory.
