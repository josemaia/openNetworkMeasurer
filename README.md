# openNetworkMeasurer

This is the Android client I developed for the project in my Master's dissertation. It is destined to obtain Wi-Fi/Cellular measurements from smartphones and send them to a server that will process them, also available on [GitHub](https://github.com/josemaia/openNetworkMeasurer-server).

# To run

The .apk is included in the app/ folder.

If you want to compile from source, you should make sure your local maven repository has the things in the maven-stuff folder,
as there were some necessary changes to libraries I used in order to make this work.

Furthermore, you need to implement the PrivateValues interface into a file called myPrivateValues, containing the API key of your
weatherunderground account, as well as the URL of the measurement server.

# License

Released under the MIT License. Apache license also included, to comply with a few imported libraries.
