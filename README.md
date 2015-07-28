# openNetworkMeasurer

Hello! My name is José Maia, and this is the Android client I developed for the project in my Master's dissertation.


The project is deployed at http://fedora-srv01.alunos.dcc.fc.up.pt/, although the servers have been spotty lately.

# To run

The .apk is included in the app/ folder.

If you want to compile from source, you should make sure your local maven repository has the things in the maven-stuff folder,
as there were some necessary changes to libraries I used in order to make this work.

Furthermore, you need to implement the PrivateValues interface into a file called myPrivateValues, containing the API key of your
weatherunderground account, as well as the URL of the measurement server.

# License

Released under the MIT License. Apache license also included, to comply with a few imported libraries.